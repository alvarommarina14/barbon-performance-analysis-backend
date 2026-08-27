import { colors } from '@court-proof/core';

/**
 * Phase 0 shell. Login lands here in Phase 1 and the analysis queue in Phase 2.
 */
export default function App() {
  return (
    <main className="shell">
      <p className="eyebrow">Barbon Performance</p>
      <h1>Court Proof</h1>
      <p className="lede">
        Coach workspace. Phase 0 — foundations in place: build, contract pipeline and design
        tokens.
      </p>
      <span className="pill" style={{ backgroundColor: colors.accent }}>
        Ready
      </span>
    </main>
  );
}
