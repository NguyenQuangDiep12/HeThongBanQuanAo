export const cx = (...a) => a.filter(Boolean).join(" ");

export const btn =
  "inline-flex items-center justify-center gap-2 rounded-xl px-5 py-3 text-sm font-bold transition disabled:opacity-50";

export const primary = btn + " bg-brand-800 text-white hover:bg-brand-700";

export const secondary =
  btn + " border border-line bg-white hover:border-brand-300";

export const input =
  "w-full rounded-xl border border-line bg-white px-4 py-3 text-sm outline-none focus:border-brand-500 focus:ring-4 focus:ring-brand-100";

export const label =
  "mb-2 block text-[10px] font-extrabold uppercase tracking-[.14em] text-slate-500";
