/**
 * enrich-products.mjs
 *
 * Uses Claude AI to study every product in the database and fill in:
 *   - industryIds  (which business sectors use this product)
 *   - keywords     (search terms customers would type)
 *   - description  (clear, customer-friendly copy)
 *   - isFastMoving (flag for popular everyday items)
 *   - tags         (Featured / Trending as appropriate)
 *
 * Usage:
 *   ADMIN_EMAIL=you@email.com ADMIN_PASSWORD=yourpwd ANTHROPIC_API_KEY=sk-... node enrich-products.mjs
 *
 * Optional overrides:
 *   API_BASE=https://... (defaults to Railway production)
 *   DRY_RUN=true        (logs changes without writing to DB)
 *   BATCH_SIZE=8        (products per Claude call, default 8)
 */

import Anthropic from "@anthropic-ai/sdk";

// ── Config ─────────────────────────────────────────────────────────────────
const API_BASE    = process.env.API_BASE ?? "https://moments-packaging-latest-backend-production.up.railway.app";
const EMAIL       = process.env.ADMIN_EMAIL;
const PASSWORD    = process.env.ADMIN_PASSWORD;
const CLAUDE_KEY  = process.env.ANTHROPIC_API_KEY;
const DRY_RUN     = process.env.DRY_RUN === "true";
const BATCH_SIZE  = parseInt(process.env.BATCH_SIZE ?? "8", 10);

if (!EMAIL || !PASSWORD || !CLAUDE_KEY) {
  console.error("❌  Set ADMIN_EMAIL, ADMIN_PASSWORD, and ANTHROPIC_API_KEY env vars.");
  process.exit(1);
}

const ai = new Anthropic({ apiKey: CLAUDE_KEY });

// ── Helpers ─────────────────────────────────────────────────────────────────
async function apiFetch(path, { token, method = "GET", body } = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    throw new Error(`${method} ${path} → ${res.status}: ${txt.slice(0, 200)}`);
  }
  return res.status === 204 ? null : res.json();
}

async function getAllProducts(token) {
  const products = [];
  let page = 0;
  while (true) {
    const data = await apiFetch(`/api/v1/admin/products?page=${page}&size=50`, { token });
    const content = data.content ?? data.rows ?? data;
    products.push(...content);
    const totalPages = data.totalPages ?? 1;
    if (page + 1 >= totalPages || content.length === 0) break;
    page++;
  }
  return products;
}

async function getIndustries(token) {
  const data = await apiFetch("/api/v1/public/industries", { token });
  return Array.isArray(data) ? data : (data.content ?? []);
}

// ── Claude enrichment ────────────────────────────────────────────────────────
async function enrichBatch(products, industries) {
  const industryList = industries.map(i => `"${i.slug}" (${i.name})`).join(", ");

  const prompt = `You are a packaging product expert for "Moments Packaging (K) Limited", a Nairobi-based wholesale packaging supplier serving Kenyan businesses.

Analyze each product below and return a JSON array — one object per product — with these fields:

- "id": the product id (copy exactly from input)
- "industryIds": array of industry SLUGS most relevant for this product. Pick from: [${industryList}]. A product can belong to multiple industries. Be generous — if bakeries, cafes, AND food manufacturers all use this product, include all three slugs.
- "keywords": array of 6-12 search terms a Kenyan business owner would type to find this product. Include: common local names, materials, sizes, use-cases, and Swahili terms where natural (e.g. "mfuko", "karatasi", "mkoba").
- "description": a concise 1-2 sentence customer-facing description (max 180 chars). Focus on who uses it and why. If the existing description is already good, improve it slightly for clarity.
- "isFastMoving": true if this is a high-volume everyday item (bags, cups, common packaging), false for niche/specialty items.
- "tags": array — include "Featured" if it's a flagship versatile product; include "Trending" if it's a currently popular category in Kenya. Most products get an empty array.

Products to analyse:
${JSON.stringify(products.map(p => ({
  id: p.id,
  name: p.name,
  category: p.category,
  description: p.description ?? "",
})), null, 2)}

Return ONLY a valid JSON array. No explanation, no markdown fences.`;

  const message = await ai.messages.create({
    model: "claude-haiku-4-5-20251001",
    max_tokens: 4096,
    messages: [{ role: "user", content: prompt }],
  });

  const text = message.content[0].text.trim();
  // Strip markdown fences if Claude adds them despite instructions
  const clean = text.replace(/^```(?:json)?\n?/i, "").replace(/\n?```$/i, "").trim();
  return JSON.parse(clean);
}

// ── Main ─────────────────────────────────────────────────────────────────────
async function main() {
  console.log(`\n🤖  Moments Product Enrichment — ${DRY_RUN ? "DRY RUN" : "LIVE"}\n`);

  // 1. Login
  console.log("🔐  Logging in as admin…");
  const auth = await apiFetch("/api/v1/auth/login", {
    method: "POST",
    body: { email: EMAIL, password: PASSWORD },
  });
  const token = auth.accessToken ?? auth.token;
  console.log("    ✓ Authenticated\n");

  // 2. Load industries (for slug→id mapping)
  const industries = await getIndustries(token);
  console.log(`📦  Industries available: ${industries.map(i => i.slug).join(", ")}\n`);
  const slugToId = Object.fromEntries(industries.map(i => [i.slug, i.id]));

  // 3. Load all products
  console.log("📋  Fetching all products…");
  const products = await getAllProducts(token);
  console.log(`    Found ${products.length} products\n`);

  if (products.length === 0) {
    console.log("⚠️  No products found. Is the DB seeded?");
    return;
  }

  // 4. Process in batches
  let updated = 0, failed = 0;
  for (let i = 0; i < products.length; i += BATCH_SIZE) {
    const batch = products.slice(i, i + BATCH_SIZE);
    const batchNum = Math.floor(i / BATCH_SIZE) + 1;
    const totalBatches = Math.ceil(products.length / BATCH_SIZE);
    process.stdout.write(`🧠  Batch ${batchNum}/${totalBatches} — asking Claude about: ${batch.map(p => p.name).join(", ")}`);
    process.stdout.write("\n");

    let enriched;
    try {
      enriched = await enrichBatch(batch, industries);
    } catch (err) {
      console.error(`\n    ❌  Claude error on batch ${batchNum}: ${err.message}`);
      // Retry once with smaller context
      try {
        console.log("    ↩️  Retrying batch individually…");
        enriched = [];
        for (const p of batch) {
          const single = await enrichBatch([p], industries);
          enriched.push(...single);
        }
      } catch (retryErr) {
        console.error(`    ❌  Retry failed: ${retryErr.message}`);
        failed += batch.length;
        continue;
      }
    }

    // 5. Map slugs → UUIDs and update each product
    for (const result of enriched) {
      const product = batch.find(p => p.id === result.id);
      if (!product) { console.warn(`    ⚠️  Unknown id ${result.id} in Claude response`); continue; }

      const industryIds = (result.industryIds ?? [])
        .map(slug => slugToId[slug])
        .filter(Boolean);

      const payload = {
        description:  result.description  ?? product.description,
        keywords:     result.keywords     ?? [],
        isFastMoving: result.isFastMoving ?? product.isFastMoving ?? false,
        tags:         result.tags         ?? product.tags ?? [],
        industryIds,
      };

      if (DRY_RUN) {
        console.log(`    [DRY] ${product.name}`);
        console.log(`          industries: ${result.industryIds?.join(", ") ?? "none"}`);
        console.log(`          keywords:   ${payload.keywords.join(", ")}`);
        console.log(`          desc:       ${payload.description}`);
        updated++;
        continue;
      }

      try {
        await apiFetch(`/api/v1/admin/products/${result.id}`, {
          token,
          method: "PUT",
          body: payload,
        });
        console.log(`    ✓ ${product.name} → [${result.industryIds?.join(", ") ?? ""}]`);
        updated++;
      } catch (err) {
        console.error(`    ❌ Failed to update "${product.name}": ${err.message}`);
        failed++;
      }
    }

    // Rate-limit: brief pause between batches to avoid hammering the API
    if (i + BATCH_SIZE < products.length) {
      await new Promise(r => setTimeout(r, 800));
    }
  }

  console.log(`\n✅  Done — ${updated} updated, ${failed} failed\n`);
}

main().catch(err => {
  console.error("Fatal error:", err);
  process.exit(1);
});
