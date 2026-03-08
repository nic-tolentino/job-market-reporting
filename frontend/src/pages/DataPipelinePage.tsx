import { useState } from 'react';
import Mermaid from '../components/common/Mermaid';
import { pipelineDiagram } from '../constants/pipelineDiagram';
import { Database, Zap, Shield, ArrowRight, Info } from 'lucide-react';

const stages = [
  {
    id: 'bronze',
    title: 'Bronze Layer',
    icon: <Database className="w-5 h-5 text-red-400" />,
    color: 'border-red-500/50 text-red-400 bg-red-500/5',
    description: 'The raw ingestion layer. Contains immutable JSON payloads exactly as received from external scrapers and ATS systems.',
    benefits: ['Point-in-time recovery', 'Audit traceability', 'Source of truth for reprocessing']
  },
  {
    id: 'silver',
    title: 'Silver Layer',
    icon: <Shield className="w-5 h-5 text-emerald-400" />,
    color: 'border-emerald-500/50 text-emerald-400 bg-emerald-500/5',
    description: 'The structured and cleaned layer. Data is deduplicated, classified (tech vs non-tech), and merged into stable entities.',
    benefits: ['Cleaned duplicates', 'Standardized schemas', 'Enriched metadata']
  },
  {
    id: 'api',
    title: 'BFF Layer',
    icon: <Zap className="w-5 h-5 text-blue-400" />,
    color: 'border-blue-500/50 text-blue-400 bg-blue-500/5',
    description: 'The Backend-for-Frontend layer. Aggregates and caches data for fast retrieval by the React client.',
    benefits: ['Low latency caching', 'Domain-specific DTOs', 'Optimized query patterns']
  }
];

export default function DataPipelinePage() {
  const [activeStage, setActiveStage] = useState<string | null>(null);

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl animate-in fade-in duration-700">
      <header className="mb-12">
        <h1 className="text-4xl font-extrabold tracking-tight text-primary mb-4">
          Data Pipeline <span className="text-secondary opacity-50 font-normal">Visualiser</span>
        </h1>
        <p className="text-lg text-secondary max-w-2xl leading-relaxed">
          An interactive overview of how job market data flows from external sources through our 
          <span className="text-accent font-medium"> Medallion Architecture</span> into the insights you see on screen.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-12">
        {stages.map((stage) => (
          <div 
            key={stage.id}
            onMouseEnter={() => setActiveStage(stage.id)}
            onMouseLeave={() => setActiveStage(null)}
            className={`p-6 rounded-2xl border transition-all duration-300 transform cursor-default
              ${activeStage === stage.id ? 'scale-[1.02] shadow-theme-lg z-10 ' + stage.color.split(' ')[0] : 'border-border bg-card/50 shadow-theme-sm'}
            `}
          >
            <div className="flex items-center gap-3 mb-4">
              <div className={`p-2 rounded-lg ${stage.color.split(' ').slice(1).join(' ')}`}>
                {stage.icon}
              </div>
              <h3 className="text-lg font-bold text-primary">{stage.title}</h3>
            </div>
            <p className="text-sm text-secondary mb-4 min-h-[60px]">
              {stage.description}
            </p>
            <ul className="space-y-2">
              {stage.benefits.map((benefit, i) => (
                <li key={i} className="flex items-center gap-2 text-xs text-secondary/80">
                  <ArrowRight className="w-3 h-3 text-accent" />
                  {benefit}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <section className="relative group">
        <div className="absolute inset-0 bg-gradient-to-r from-accent/5 to-purple-500/5 blur-3xl rounded-full -z-10 opacity-50 group-hover:opacity-100 transition-opacity" />
        
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-surface-hover border border-border">
            <Info className="w-4 h-4 text-accent" />
            <span className="text-xs font-semibold uppercase tracking-wider text-secondary">Interactive Sequence Diagram</span>
          </div>
          <div className="hidden sm:flex gap-4">
            <span className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest text-secondary/50">
              <span className="w-2 h-2 rounded-full bg-blue-400" /> API
            </span>
            <span className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest text-secondary/50">
              <span className="w-2 h-2 rounded-full bg-emerald-400" /> Processing
            </span>
            <span className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest text-secondary/50">
              <span className="w-2 h-2 rounded-full bg-red-400" /> Bronze
            </span>
          </div>
        </div>

        <div className="bg-card/30 backdrop-blur-sm rounded-3xl p-2 border border-border shadow-theme-xl">
          <Mermaid chart={pipelineDiagram} className="min-h-[800px] w-full" />
        </div>
      </section>

      <footer className="mt-16 pt-8 border-t border-border flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div>
          <h4 className="font-bold text-primary mb-1">Architecture Documentation</h4>
          <p className="text-sm text-secondary">
            Generated directly from the system's technical specification.
          </p>
        </div>
        <div className="flex gap-4">
          <button 
            onClick={() => window.open('/docs/data/diagrams/data-pipeline-flowchart.md', '_blank')}
            className="px-4 py-2 rounded-lg bg-surface-hover border border-border text-xs font-bold hover:bg-surface-hover/80 transition-colors"
          >
            View Raw Markdown
          </button>
        </div>
      </footer>
    </div>
  );
}
