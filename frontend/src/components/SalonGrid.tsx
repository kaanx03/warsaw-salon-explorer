"use client";

import { motion } from "framer-motion";
import SalonCard from "./SalonCard";
import type { SalonListItem } from "@/lib/types";

export default function SalonGrid({ salons }: { salons: SalonListItem[] }) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-x-7 gap-y-9">
      {salons.map((salon, i) => (
        <motion.div
          key={salon.id}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: 0.5,
            delay: Math.min(i * 0.04, 0.32),
            ease: [0.22, 1, 0.36, 1],
          }}
        >
          <SalonCard salon={salon} />
        </motion.div>
      ))}
    </div>
  );
}
