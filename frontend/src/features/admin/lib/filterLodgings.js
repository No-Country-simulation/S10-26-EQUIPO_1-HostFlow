export function filterLodgings(lodgings, query) {
  const q = query.trim().toLowerCase();
  if (!q) return lodgings;
  return lodgings.filter((l) =>
    `${l.owner || ""} ${l.ownerEmail || ""} ${l.id || ""} ${l.name || ""}`.toLowerCase().includes(q),
  );
}
