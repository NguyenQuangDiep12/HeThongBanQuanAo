export function Toast({ message }) {
  return (
    message && (
      <div className="fixed bottom-5 right-5 z-[80] rounded-xl bg-ink px-5 py-4 text-sm font-semibold text-white shadow-xl">
        {message}
      </div>
    )
  );
}
