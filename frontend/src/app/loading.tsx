export default function Loading() {
  return (
    <div className="max-w-[1280px] mx-auto px-6 lg:px-16 pt-[200px] pb-24">
      <div className="columns-1 sm:columns-2 lg:columns-3 xl:columns-4 gap-5 space-y-5">
        {[...Array(8)].map((_, i) => (
          <div key={i} className="break-inside-avoid">
            <div
              className={`rounded-2xl bg-[#e4e2e1] animate-pulse ${i % 3 === 0 ? "aspect-[3/4]" : "aspect-[4/5]"}`}
            />
            <div className="mt-3 px-1 flex gap-2">
              <div className="h-3 w-12 bg-[#e4e2e1] rounded-full animate-pulse" />
              <div className="h-3 w-20 bg-[#ede9e8] rounded-full animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
