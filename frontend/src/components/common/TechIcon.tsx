import { useEffect, useState } from 'react';
import { useTheme } from 'next-themes';
import { TECH_BRAND_COLORS } from '../../constants/techBrandColors';

interface TechIconProps {
  techId: string;
  className?: string;
}

export function TechIcon({ techId, className = 'w-12 h-12' }: TechIconProps) {
  const { resolvedTheme } = useTheme();
  const [svgContent, setSvgContent] = useState<string | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!techId) return;

    fetch(`/icons/tech/${techId.toLowerCase()}.svg`)
      .then(r => r.ok ? r.text() : Promise.reject('Not found'))
      .then(text => {
        // Simple sanitization: only keep the SVG tag and its contents
        const svgMatch = text.match(/<svg[^>]*>([\s\S]*?)<\/svg>/i);
        if (svgMatch) {
          setSvgContent(svgMatch[0]);
        } else {
          setError(true);
        }
      })
      .catch(() => setError(true));
  }, [techId]);

  if (error || !svgContent) {
    // Fallback: simple colored circle with first letter if SVG fails
    const initial = techId?.charAt(0).toUpperCase() || '?';
    return (
      <div className={`${className} rounded-lg bg-inset border border-border flex items-center justify-center text-secondary font-bold text-lg`}>
        {initial}
      </div>
    );
  }

  const colors = TECH_BRAND_COLORS[techId.toLowerCase()];
  const fill = colors
    ? (resolvedTheme === 'dark' ? colors.dark : colors.light)
    : (resolvedTheme === 'dark' ? '#F1F5F9' : '#0F172A'); // Fallback to theme primary

  // Inject fill into the SVG. We replace the opening <svg tag to include the fill attribute.
  // Note: Simple Icons SVGs don't usually have a fill attribute on the root <svg> or paths.
  const tinted = svgContent.replace(/<svg\s+/i, `<svg fill="${fill}" `);

  return (
    <div
      className={`${className} flex items-center justify-center`}
      dangerouslySetInnerHTML={{ __html: tinted }}
    />
  );
}
