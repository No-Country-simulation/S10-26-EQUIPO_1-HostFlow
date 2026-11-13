import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@utils/format";
import { Reveal } from "./Reveal";
import { faqs } from "../lib/faqs";

// ── FAQ ─────────────────────────────────────────────────────────────────────

export function FAQ() {
  const [open, setOpen] = useState(0);
  return (
    <section className="mx-auto max-w-3xl px-5 py-20 sm:px-6 lg:py-28">
      <Reveal className="mb-12 text-center">
        <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
          FAQ
        </span>
        <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">Preguntas frecuentes</h2>
      </Reveal>

      <div className="grid gap-3">
        {faqs.map((faq, index) => {
          const isOpen = open === index;
          return (
            <Reveal key={faq.q} delay={index * 70}>
              <div className="card overflow-hidden">
                <button
                  id={`faq-btn-${index}`}
                  onClick={() => setOpen(isOpen ? -1 : index)}
                  aria-expanded={isOpen}
                  aria-controls={`faq-panel-${index}`}
                  className="flex w-full cursor-pointer items-center justify-between gap-4 px-6 py-4 text-left"
                >
                  <span className="font-bold text-ink">{faq.q}</span>
                  <ChevronDown size={18} aria-hidden="true" className={cn("shrink-0 text-muted transition-transform duration-300", isOpen && "rotate-180")} />
                </button>
                {isOpen && (
                  <p id={`faq-panel-${index}`} aria-labelledby={`faq-btn-${index}`} className="px-6 pb-5 text-sm leading-relaxed text-muted animate-fade">
                    {faq.a}
                  </p>
                )}
              </div>
            </Reveal>
          );
        })}
      </div>
    </section>
  );
}
