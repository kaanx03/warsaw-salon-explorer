export default function Loading() {
  return (
    <div className="pt-[72px] min-h-screen bg-[#fcf9f8]">
      <div className="max-w-[1280px] mx-auto px-6 lg:px-16 py-10">
        <div className="h-4 w-16 bg-[#e4e2e1] rounded-full animate-pulse mb-8" />
        <div className="grid lg:grid-cols-[1fr_360px] gap-10">
          <div>
            <div className="rounded-3xl aspect-[16/9] bg-[#e4e2e1] animate-pulse" />
            <div className="mt-8 flex flex-col gap-3">
              <div className="h-3 w-20 bg-[#ede9e8] rounded-full animate-pulse" />
              <div className="h-10 w-3/4 bg-[#e4e2e1] rounded-xl animate-pulse" />
              <div className="h-5 w-32 bg-[#ede9e8] rounded-full animate-pulse" />
            </div>
          </div>
          <div className="hidden lg:block">
            <div className="rounded-3xl h-80 bg-[#e4e2e1] animate-pulse" />
          </div>
        </div>
      </div>
    </div>
  );
}
