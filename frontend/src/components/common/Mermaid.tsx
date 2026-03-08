import React, { useEffect, useRef, useState } from 'react';
import mermaid from 'mermaid';
import { ZoomIn, ZoomOut, RefreshCw, Maximize, X } from 'lucide-react';

interface MermaidProps {
  chart: string;
  id?: string;
  className?: string;
  onNodeClick?: (nodeId: string, nodeText: string) => void;
}

// Initialize mermaid
mermaid.initialize({
  startOnLoad: false,
  theme: 'base',
  securityLevel: 'loose',
  fontFamily: 'Outfit, Inter, system-ui, sans-serif',
  themeVariables: {
    primaryColor: '#1e293b',
    primaryTextColor: '#f8fafc',
    primaryBorderColor: '#334155',
    lineColor: '#6366f1',
    secondaryColor: '#1e293b',
    tertiaryColor: '#0f172a',
    fontSize: '14px',
    fontWeight: 500,
    edgeLabelBackground: '#020617',
    nodeBorder: '#475569',
    clusterBkg: '#0f172a',
    clusterBorder: '#334155',
    titleColor: '#e2e8f0',
  }
});

interface TooltipData {
  x: number;
  y: number;
  title: string;
  description: string;
}

const Mermaid: React.FC<MermaidProps> = ({ chart, id = 'mermaid-chart', className = '', onNodeClick }) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [scale, setScale] = useState(1);
  const [isDragging, setIsDragging] = useState(false);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  const [tooltip, setTooltip] = useState<TooltipData | null>(null);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);

  // Node descriptions for tooltips
  const nodeDescriptions: Record<string, string> = {
    'LinkedIn': 'External job board - primary data source',
    'ApifyScraper': 'Web scraping service that extracts job data',
    'ATS': 'Applicant Tracking Systems (Greenhouse, Lever, etc.)',
    'ManualData': 'companies.json - master company manifest',
    'Webhook': 'Apify webhook controller for data changes',
    'JobDataSync': 'Orchestrates data sync pipeline',
    'RawIngestion': 'Bronze layer - immutable raw data storage',
    'RawJobs': 'Silver layer - deduplicated job records',
    'RawCompanies': 'Silver layer - company records',
    'LandingAPI': 'BFF endpoint for landing page data',
    'LandingPage': 'Main landing page with global stats',
    'TechPage': 'Technology details page',
    'CompanyPage': 'Company profile page',
    'JobPage': 'Job details page',
  };

  useEffect(() => {
    const renderChart = async () => {
      if (!chartRef.current || !chart) return;

      try {
        setError(null);
        chartRef.current.innerHTML = '';

        const uniqueId = `${id}-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
        const { svg } = await mermaid.render(uniqueId, chart);
        chartRef.current.innerHTML = svg;

        const svgElement = chartRef.current.querySelector('svg');
        if (svgElement) {
          svgElement.style.cursor = 'grab';
          svgElement.style.userSelect = 'none';

          // Add interactivity to nodes
          const nodes = chartRef.current.querySelectorAll('.node, .label-container, .label, .cluster');
          nodes.forEach(node => {
            node.addEventListener('click', (e) => {
              e.stopPropagation();
              handleNodeClick(e as unknown as MouseEvent, node);
            });
            node.addEventListener('mouseenter', (e) => {
              handleNodeHover(e as unknown as MouseEvent, node);
            });
            node.addEventListener('mouseleave', () => {
              handleNodeLeave();
            });
          });

          // Add double-click to collapse/expand subgraphs
          const clusters = chartRef.current.querySelectorAll('.cluster');
          clusters.forEach(cluster => {
            cluster.addEventListener('dblclick', (e) => {
              e.stopPropagation();
              handleSubgraphDoubleClick(e as unknown as MouseEvent, cluster);
            });
          });
        }

        setScale(1);
        setPan({ x: 0, y: 0 });
        if (svgElement) {
          svgElement.style.transform = 'translate(0px, 0px) scale(1)';
          svgElement.style.transformOrigin = 'center center';
        }
      } catch (err) {
        console.error('Mermaid render error:', err);
        setError(err instanceof Error ? err.message : 'Failed to render diagram');
      }
    };

    renderChart();
  }, [chart, id]);

  const handleNodeClick = (_e: React.MouseEvent | MouseEvent, node: Element) => {
    const nodeId = (node as HTMLElement).id || 'unknown';
    const nodeText = node.textContent?.trim() || '';
    
    // Highlight selected node
    setSelectedNode(nodeId);
    
    // Remove highlight from all nodes
    chartRef.current?.querySelectorAll('.node').forEach(n => {
      (n as HTMLElement).style.filter = 'none';
      (n as HTMLElement).style.opacity = '1';
    });
    
    // Highlight selected node and dim others
    chartRef.current?.querySelectorAll('.node').forEach(n => {
      if (n !== node) {
        (n as HTMLElement).style.opacity = '0.3';
      } else {
        (n as HTMLElement).style.filter = 'drop-shadow(0 0 8px rgba(99, 102, 241, 0.8))';
      }
    });

    if (onNodeClick) {
      onNodeClick(nodeId, nodeText);
    }
  };

  const handleNodeHover = (_e: React.MouseEvent | MouseEvent, node: Element) => {
    const nodeText = node.textContent?.trim() || '';
    const description = nodeDescriptions[nodeText] || 'Click to highlight this component';
    
    const rect = (node as HTMLElement).getBoundingClientRect();
    const containerRect = containerRef.current?.getBoundingClientRect();
    
    if (containerRect) {
      setTooltip({
        x: rect.left - containerRect.left + rect.width / 2,
        y: rect.top - containerRect.top - 10,
        title: nodeText,
        description,
      });
    }
    
    (node as HTMLElement).style.opacity = '0.7';
  };

  const handleNodeLeave = () => {
    setTooltip(null);
    chartRef.current?.querySelectorAll('.node').forEach(node => {
      if ((node as HTMLElement).id !== selectedNode) {
        (node as HTMLElement).style.opacity = '1';
      }
    });
  };

  const handleSubgraphDoubleClick = (_e: MouseEvent, cluster: Element) => {
    // Toggle visibility
    const isVisible = cluster.getAttribute('data-collapsed') !== 'true';
    cluster.setAttribute('data-collapsed', String(!isVisible));
    (cluster as HTMLElement).style.display = isVisible ? 'none' : 'block';
  };

  const clearSelection = () => {
    setSelectedNode(null);
    chartRef.current?.querySelectorAll('.node').forEach(n => {
      (n as HTMLElement).style.filter = 'none';
      (n as HTMLElement).style.opacity = '1';
    });
  };

  // Pan handling
  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.button !== 0) return;
    setIsDragging(true);
    setStartPos({ x: e.clientX - pan.x, y: e.clientY - pan.y });
    const svgElement = chartRef.current?.querySelector('svg');
    if (svgElement) svgElement.style.cursor = 'grabbing';
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;
    e.preventDefault();
    const newX = e.clientX - startPos.x;
    const newY = e.clientY - startPos.y;
    setPan({ x: newX, y: newY });
    updateTransform();
  };

  const handleMouseUp = () => {
    setIsDragging(false);
    const svgElement = chartRef.current?.querySelector('svg');
    if (svgElement) svgElement.style.cursor = 'grab';
  };

  const handleWheel = (e: React.WheelEvent) => {
    if (!e.ctrlKey && !e.metaKey) return;
    e.preventDefault();
    e.stopPropagation();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    const newScale = Math.min(Math.max(scale + delta, 0.3), 3);
    setScale(newScale);
    updateTransform(newScale);
  };

  const updateTransform = (newScale?: number) => {
    const svgElement = chartRef.current?.querySelector('svg');
    if (svgElement) {
      const s = newScale ?? scale;
      svgElement.style.transform = `translate(${pan.x}px, ${pan.y}px) scale(${s})`;
    }
  };

  const handleZoomIn = () => {
    const newScale = Math.min(scale + 0.2, 3);
    setScale(newScale);
    updateTransform(newScale);
  };

  const handleZoomOut = () => {
    const newScale = Math.max(scale - 0.2, 0.3);
    setScale(newScale);
    updateTransform(newScale);
  };

  const handleReset = () => {
    setScale(1);
    setPan({ x: 0, y: 0 });
    clearSelection();
    updateTransform(1);
  };

  return (
    <div
      ref={containerRef}
      className={`relative rounded-xl bg-surface/50 border border-border shadow-theme-lg overflow-hidden ${className}`}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      onWheel={handleWheel}
      onClick={clearSelection}
    >
      {/* Controls */}
      <div className="absolute top-3 right-3 z-10 flex flex-col gap-2">
        <button
          onClick={handleZoomIn}
          className="p-2 rounded-lg bg-card/80 backdrop-blur border border-border hover:bg-surface-hover transition-colors"
          title="Zoom in (Ctrl + Scroll)"
        >
          <ZoomIn className="w-4 h-4 text-primary" />
        </button>
        <button
          onClick={handleZoomOut}
          className="p-2 rounded-lg bg-card/80 backdrop-blur border border-border hover:bg-surface-hover transition-colors"
          title="Zoom out (Ctrl + Scroll)"
        >
          <ZoomOut className="w-4 h-4 text-primary" />
        </button>
        <button
          onClick={handleReset}
          className="p-2 rounded-lg bg-card/80 backdrop-blur border border-border hover:bg-surface-hover transition-colors"
          title="Reset view"
        >
          <RefreshCw className="w-4 h-4 text-primary" />
        </button>
        <button
          onClick={() => {
            const svgElement = chartRef.current?.querySelector('svg');
            if (svgElement && containerRef.current) {
              const rect = svgElement.getBoundingClientRect();
              const containerRect = containerRef.current.getBoundingClientRect();
              const newScale = Math.min(containerRect.width / rect.width, containerRect.height / rect.height, 1);
              setScale(newScale);
              setPan({ x: (containerRect.width - rect.width * newScale) / 2, y: (containerRect.height - rect.height * newScale) / 2 });
              updateTransform(newScale);
            }
          }}
          className="p-2 rounded-lg bg-card/80 backdrop-blur border border-border hover:bg-surface-hover transition-colors"
          title="Fit to screen"
        >
          <Maximize className="w-4 h-4 text-primary" />
        </button>
        {selectedNode && (
          <button
            onClick={clearSelection}
            className="p-2 rounded-lg bg-accent/20 border border-accent hover:bg-accent/30 transition-colors"
            title="Clear selection"
          >
            <X className="w-4 h-4 text-accent" />
          </button>
        )}
      </div>

      {/* Tooltip */}
      {tooltip && (
        <div
          className="absolute z-20 pointer-events-none transform -translate-x-1/2 -translate-y-full"
          style={{ left: tooltip.x, top: tooltip.y }}
        >
          <div className="bg-card border border-border rounded-lg shadow-2xl p-3 max-w-xs">
            <p className="font-semibold text-primary text-sm mb-1">{tooltip.title}</p>
            <p className="text-xs text-secondary/80">{tooltip.description}</p>
            <div className="absolute bottom-0 left-1/2 transform -translate-x-1/2 translate-y-1/2 rotate-45 w-2 h-2 bg-card border-r border-b border-border" />
          </div>
        </div>
      )}

      {/* Chart */}
      <div
        ref={chartRef}
        className="w-full h-full min-h-[800px] flex items-center justify-center p-4"
      />

      {/* Help tooltip */}
      <div className="absolute bottom-3 left-3 z-10 px-3 py-2 rounded-lg bg-card/80 backdrop-blur border border-border text-xs text-secondary/70">
        <span className="hidden sm:inline">🖱️ Drag to pan • Ctrl/Cmd + Scroll to zoom • Click to highlight • Double-click subgraphs</span>
        <span className="sm:hidden">👆 Drag to pan • Tap to highlight</span>
      </div>

      {/* Error display */}
      {error && (
        <div className="absolute inset-0 flex items-center justify-center bg-red-500/10 backdrop-blur-sm">
          <div className="text-center p-8">
            <p className="text-red-400 font-semibold mb-2">Failed to render diagram</p>
            <p className="text-red-300/70 text-sm">{error}</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default Mermaid;
