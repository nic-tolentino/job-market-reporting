interface CompanyLogoProps {
    logoUrl?: string | null;
    companyName: string;
    className?: string;     // Additional classes for the container (e.g., size, borders)
    imageClassName?: string; // Additional classes for the img tag (e.g., padding)
}

export default function CompanyLogo({
    logoUrl,
    companyName,
    className = '',
    imageClassName = ''
}: CompanyLogoProps) {
    const isUrl = logoUrl?.startsWith('http');
    const fallbackText = logoUrl && !isUrl ? logoUrl : companyName.charAt(0);

    return (
        <div className={`flex items-center justify-center bg-card font-bold text-secondary overflow-hidden ${className}`}>
            {isUrl ? (
                <img
                    src={logoUrl ?? undefined}
                    alt={`${companyName} logo`}
                    className={`h-full w-full object-contain ${imageClassName}`}
                />
            ) : (
                fallbackText
            )}
        </div>
    );
}
