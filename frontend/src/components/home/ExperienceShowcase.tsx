import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Sparkles, Target, Clock, Zap } from 'lucide-react';

interface Metric {
  id: string;
  label: string;
  description: string;
  value: number;
  max?: number;
  suffix?: string;
  type: 'progress' | 'meter' | 'number';
}

const METRICS: Metric[] = [
  {
    id: 'on-time',
    label: 'On-time Deliveries',
    description: 'Real-time dispatching keeps 95% of packages on schedule.',
    value: 95,
    max: 100,
    type: 'progress',
    suffix: '%',
  },
  {
    id: 'customer-happiness',
    label: 'Customer Satisfaction',
    description: 'Post-delivery feedback averaged across last 30 days.',
    value: 4.7,
    max: 5,
    type: 'meter',
  },
  {
    id: 'dispatch-speed',
    label: 'Dispatch Speed',
    description: 'Average time to assign a truck after order confirmation.',
    value: 12,
    max: 60,
    type: 'progress',
    suffix: ' min',
  },
  {
    id: 'energy-saved',
    label: 'Miles Saved',
    description: 'Optimized routing reduced mileage on today\'s routes.',
    value: 184,
    type: 'number',
    suffix: ' mi',
  },
];

const ICONS = [Sparkles, Target, Clock, Zap];

export const ExperienceShowcase: React.FC = () => {
  const sectionRef = useRef<HTMLElement | null>(null);
  const [isActivated, setIsActivated] = useState(false);
  const [animatedValues, setAnimatedValues] = useState(() => METRICS.map(() => 0));
  const prefersReducedMotion = usePrefersReducedMotion();

  useEffect(() => {
    const sectionEl = sectionRef.current;
    if (!sectionEl) {
      return;
    }

    const cards = Array.from(sectionEl.querySelectorAll<HTMLElement>('[data-fade-in]'));
    const updateDataset = (active: boolean) => {
      sectionEl.dataset.visible = active ? 'true' : 'false';
      cards.forEach((card, index) => {
        window.requestAnimationFrame(() => {
          card.style.setProperty('--enter-delay', `${index * 70}ms`);
          card.dataset.active = active ? 'true' : 'false';
        });
      });
    };

    if (prefersReducedMotion) {
      updateDataset(true);
      setIsActivated(true);
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            updateDataset(true);
            setIsActivated(true);
            observer.disconnect();
          }
        });
      },
      { threshold: 0.4 }
    );

    observer.observe(sectionEl);

    return () => observer.disconnect();
  }, [prefersReducedMotion]);

  useEffect(() => {
    if (!isActivated) {
      return;
    }

    let frameId: number;
    const start = performance.now();
    const duration = 1200;

    const animate = (time: number) => {
      const progress = Math.min((time - start) / duration, 1);
      setAnimatedValues(
        METRICS.map((metric) => {
          if (metric.type === 'number') {
            return Math.round(metric.value * progress);
          }

          if (metric.max) {
            return metric.value * progress;
          }

          return metric.value;
        })
      );

      if (progress < 1) {
        frameId = window.requestAnimationFrame(animate);
      }
    };

    frameId = window.requestAnimationFrame(animate);

    return () => window.cancelAnimationFrame(frameId);
  }, [isActivated]);

  const renderedMetrics = useMemo(() => {
    return METRICS.map((metric, index) => {
      const Icon = ICONS[index % ICONS.length];
      const animatedValue = animatedValues[index];

      return (
        <article
          key={metric.id}
          className="experience-card"
          data-fade-in
          aria-labelledby={`${metric.id}-label`}
        >
          <header className="experience-card__header">
            <div className="experience-card__icon" aria-hidden="true">
              <Icon className="h-5 w-5" />
            </div>
            <h3 id={`${metric.id}-label`} className="experience-card__title">
              {metric.label}
            </h3>
          </header>

          <div className="experience-card__body">
            {renderMetricValue(metric, animatedValue)}
          </div>

          <footer className="experience-card__footer">
            <p className="experience-card__description">{metric.description}</p>
          </footer>
        </article>
      );
    });
  }, [animatedValues]);

  return (
    <section
      ref={sectionRef}
      className="experience-section animate-gradient-flow"
      data-experience-section
      aria-labelledby="experience-section-heading"
    >
      <header className="experience-section__header">
        <p className="experience-section__eyebrow">Everyday usability improvements</p>
        <h2 id="experience-section-heading" className="experience-section__title">
          Operations that feel fast, transparent, and assistive
        </h2>
        <p className="experience-section__subtitle">
          HTML5 semantics, native inputs, and purposeful micro-interactions keep dispatch teams focused and confident.
        </p>
      </header>

      <div className="experience-section__grid" role="list">
        {renderedMetrics}
      </div>

      <aside className="experience-section__aside">
        <details className="experience-section__details" data-fade-in>
          <summary className="experience-section__summary">Productivity shortcuts</summary>
          <ul className="experience-section__tips" aria-label="Keyboard shortcuts">
            <li>
              <kbd>/</kbd> Focus the tracking field instantly
            </li>
            <li>
              <kbd>Ctrl</kbd> + <kbd>Enter</kbd> Submit shipment forms from anywhere
            </li>
            <li>
              <kbd>Alt</kbd> + <kbd>H</kbd> Toggle the help panel
            </li>
          </ul>
        </details>

        <figure className="experience-section__figure" data-fade-in>
          <figcaption className="experience-section__caption">
            Delivery promise accuracy over the last 24 hours
          </figcaption>
          <progress
            value={Math.min(animatedValues[0] || 0, 100)}
            max={100}
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(animatedValues[0] || 0)}
            aria-describedby="experience-section-heading"
          />
        </figure>
      </aside>
    </section>
  );
};

function renderMetricValue(metric: Metric, animatedValue: number) {
  if (metric.type === 'progress' && metric.max) {
    return (
      <div className="experience-metric experience-metric--progress">
        <span className="experience-metric__value">
          {Math.round(animatedValue)}{metric.suffix ?? ''}
        </span>
        <progress
          value={Math.min(animatedValue, metric.max)}
          max={metric.max}
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={metric.max}
          aria-valuenow={Math.round(animatedValue)}
        />
      </div>
    );
  }

  if (metric.type === 'meter' && metric.max) {
    return (
      <div className="experience-metric experience-metric--meter">
        <span className="experience-metric__value">{animatedValue.toFixed(1)}</span>
        <meter
          min={0}
          max={metric.max}
          low={metric.max * 0.6}
          high={metric.max * 0.85}
          optimum={metric.max * 0.95}
          value={Math.min(animatedValue, metric.max)}
          aria-describedby={`${metric.id}-label`}
        />
      </div>
    );
  }

  return (
    <div className="experience-metric experience-metric--number">
      <span className="experience-metric__value">
        {Math.round(animatedValue)}{metric.suffix ?? ''}
      </span>
    </div>
  );
}

function usePrefersReducedMotion() {
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }

    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    const updatePreference = (event: MediaQueryListEvent | MediaQueryList) => {
      setPrefersReducedMotion(event.matches);
    };

    updatePreference(mediaQuery);

    mediaQuery.addEventListener('change', updatePreference);

    return () => mediaQuery.removeEventListener('change', updatePreference);
  }, []);

  return prefersReducedMotion;
}

export default ExperienceShowcase;
