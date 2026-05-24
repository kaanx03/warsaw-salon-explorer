export default function StarRating({ rating, size = "sm" }: { rating: number; size?: "sm" | "md" }) {
  const full = Math.floor(rating);
  const half = rating - full >= 0.5;
  const empty = 5 - full - (half ? 1 : 0);
  const sz = size === "md" ? "text-base" : "text-sm";

  return (
    <span className={`flex items-center gap-0.5 ${sz}`} aria-label={`${rating} out of 5`}>
      {Array.from({ length: full }).map((_, i) => (
        <span key={`f${i}`} className="text-amber-400">★</span>
      ))}
      {half && <span className="text-amber-300">★</span>}
      {Array.from({ length: empty }).map((_, i) => (
        <span key={`e${i}`} className="text-gray-200">★</span>
      ))}
      <span className="ml-1 font-semibold text-gray-700 text-xs">{rating.toFixed(1)}</span>
    </span>
  );
}
