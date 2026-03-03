import { useState } from 'react';
import { Card } from '../ui/Card';
import type { Resource } from '../../constants/techResources';

export const SpotlightCard = ({ person }: { person: Resource }) => {
    const [imageError, setImageError] = useState(false);
    const initials = person.title.split(' ').map(n => n[0]).join('').slice(0, 2);

    return (
        <Card className="p-4 hover:shadow-xl transition-all group bg-white border-slate-100/60 ring-1 ring-black/5 h-full">
            <div className="flex flex-col items-center text-center gap-3">
                <div className="relative">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-400 rotate-3 group-hover:rotate-6 transition-transform flex items-center justify-center text-white font-black text-2xl shadow-lg shadow-blue-200/50 overflow-hidden">
                        {person.previewImage && !imageError ? (
                            <img
                                src={person.previewImage}
                                alt=""
                                className="w-full h-full object-cover -rotate-3 group-hover:-rotate-6 transition-transform scale-110"
                                onError={() => setImageError(true)}
                            />
                        ) : (
                            <span>{initials}</span>
                        )}
                    </div>
                    <div className="absolute -bottom-1 -right-1 w-6 h-6 bg-white rounded-full flex items-center justify-center shadow-sm">
                        <div className="w-4 h-4 rounded-full bg-green-500 border-2 border-white"></div>
                    </div>
                </div>
                <div className="w-full px-1">
                    <h4 className="font-bold text-slate-900 group-hover:text-blue-600 transition-colors text-sm md:text-base truncate">
                        <a href={person.url} target="_blank" rel="noopener noreferrer">{person.title}</a>
                    </h4>
                    <div className="flex flex-col gap-0.5 mt-1">
                        <p className="text-[10px] uppercase font-bold tracking-wider text-slate-400 line-clamp-1">{person.description.split(',')[0] || 'Expert'}</p>
                        {person.location && (
                            <p className="text-[9px] font-medium text-blue-500/70">{person.location}</p>
                        )}
                    </div>
                </div>
            </div>
        </Card>
    );
};

interface SpotlightSectionProps {
    title: string;
    items: Resource[];
    id?: string;
}

export const SpotlightSection = ({ title, items, id }: SpotlightSectionProps) => {
    return (
        <div className="space-y-6">
            <div id={id} className="border-b border-slate-200/60 pb-3 mb-2">
                <h3 className="text-xl font-bold text-slate-900 tracking-tight">{title}</h3>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4 px-2">
                {items.map((person, idx) => (
                    <SpotlightCard key={idx} person={person} />
                ))}
            </div>
        </div>
    );
};
