export function Title({ eyebrow, title, description, children }) {
  return (
    <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-[10px] font-extrabold uppercase tracking-[.18em] text-brand-700">
          {eyebrow}
        </p>
        <h1 className="mt-2 font-display text-3xl font-extrabold tracking-[-.04em] sm:text-4xl">
          {title}
        </h1>
        {description && (
          <p className="mt-2 text-sm text-slate-500">{description}</p>
        )}
      </div>
      {children}
    </div>
  );
}
