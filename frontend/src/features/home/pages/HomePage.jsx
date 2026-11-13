import { DemoBanner } from "@components/ui";
import { Header } from "../components/Header";
import { Hero } from "../components/Hero";
import { LogoMarquee } from "../components/LogoMarquee";
import { Gallery } from "../components/Gallery";
import { Features } from "../components/Features";
import { HowItWorks } from "../components/HowItWorks";
import { Testimonial } from "../components/Testimonial";
import { Stats } from "../components/Stats";
import { Pricing } from "../components/Pricing";
import { FAQ } from "../components/FAQ";
import { FinalCTA } from "../components/FinalCTA";
import { Footer } from "../components/Footer";

export default function HomePage() {
  return (
    <div className="min-h-dvh bg-paper text-ink antialiased">
      <DemoBanner />
      <Header />
      <Hero />
      <LogoMarquee />
      <Gallery />
      <Features />
      <HowItWorks />
      <Testimonial />
      <Stats />
      <Pricing />
      <FAQ />
      <FinalCTA />
      <Footer />
    </div>
  );
}
