import { ExternalLink, Star, Users, Calendar, ChevronUp } from 'lucide-react';
import { Card, CardHeader, CardContent } from '../ui/Card';
import { H2 } from '../ui/Typography';
import { type Resource } from '../../constants/techResources';
import { useState } from 'react';

interface ResourceItemProps {
    item: Resource;
    icon: any;
}

const ResourceItem = ({ item }: ResourceItemProps) => {
    const [upvoted, setUpvoted] = useState(false);
    // Mock random base upvotes between 10 and 340 based on title length or generic random (stable-ish)
    const baseUpvotes = item.title.length * 3 + 12;

    return (
        <li className="group/item flex items-stretch hover:bg-white/60 -mx-3 rounded-xl transition-all border border-transparent hover:border-blue-100 hover:shadow-sm">

            {/* Upvote Column */}
            <button
                onClick={(e) => { e.preventDefault(); setUpvoted(!upvoted); }}
                className={`flex flex-col items-center justify-center px-3 mr-1 rounded-l-xl transition-colors ${upvoted ? 'bg-blue-50/50' : 'hover:bg-slate-100/50'}`}
            >
                <ChevronUp className={`h-5 w-5 -mb-1 transition-transform ${upvoted ? 'text-blue-600 scale-110' : 'text-slate-400'}`} />
                <span className={`text-[10px] font-bold ${upvoted ? 'text-blue-600' : 'text-slate-500'}`}>
                    {baseUpvotes + (upvoted ? 1 : 0)}
                </span>
            </button>

            <a
                href={item.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 flex items-center gap-4 py-2.5 pl-1 pr-3 transition-all"
            >
                <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                        <span className="font-semibold text-slate-900 group-hover/item:text-blue-600 transition-colors truncate">
                            {item.title}
                        </span>
                        <div className="flex items-center gap-2">
                            {item.subscribers && (
                                <span className="flex items-center gap-1 text-[10px] font-bold text-red-600 bg-red-50 px-1.5 py-0.5 rounded-md border border-red-100/50">
                                    <Users className="h-2.5 w-2.5" />
                                    {item.subscribers}
                                </span>
                            )}
                            {item.stars && (
                                <span className="flex items-center gap-1 text-[10px] font-bold text-amber-600 bg-amber-50 px-1.5 py-0.5 rounded-md border border-amber-100/50">
                                    <Star className="h-2.5 w-2.5 fill-amber-500" />
                                    {item.stars}
                                </span>
                            )}
                            {item.date && (
                                <span className="flex items-center gap-1 text-[10px] font-bold text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded-md border border-indigo-100/50">
                                    <Calendar className="h-2.5 w-2.5" />
                                    {item.date}
                                </span>
                            )}
                            <ExternalLink className="h-3 w-3 text-slate-300 opacity-0 group-hover/item:opacity-100 transition-all -translate-x-1 group-hover/item:translate-x-0" />
                        </div>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5 line-clamp-1 group-hover/item:text-slate-600">{item.description}</p>
                </div>
            </a>
        </li>
    );
};

import { ResourceModal } from './ResourceModal';

interface ResourceCardProps {
    icon: any;
    title: string;
    items?: Resource[];
    image?: string;
    featured?: boolean;
    className?: string;
    subtitle?: string;
}

export const ResourceCard = ({ icon: Icon, title, items = [], image, featured, className = "", id }: ResourceCardProps & { id?: string }) => {
    const [mainImageError, setMainImageError] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const previewItems = items.slice(0, 5);
    const hasMore = items.length > 5;

    return (
        <>
            <Card id={id} className={`overflow-hidden group flex flex-col h-full transition-all duration-300 hover:shadow-xl scroll-mt-24 ${featured ? 'md:col-span-2 bg-gradient-to-br from-blue-50 to-indigo-50 border-blue-100' : ''} ${className}`}>
                {image && !mainImageError && (
                    <div className="h-48 overflow-hidden relative">
                        <img
                            src={image}
                            alt={title}
                            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                            onError={() => setMainImageError(true)}
                        />
                        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex flex-col justify-end p-6">
                            <div className="flex items-center gap-3 text-white">
                                <div className="p-2 bg-white/20 backdrop-blur-md rounded-lg">
                                    <Icon className="h-5 w-5" />
                                </div>
                                <h3 className="text-xl font-bold">{title}</h3>
                            </div>
                        </div>
                    </div>
                )}

                {(!image || mainImageError) && (
                    <CardHeader className={`flex flex-row items-center gap-3 pb-2 ${featured ? 'pt-6 px-6' : ''}`}>
                        <div className={`p-2 rounded-lg ${featured ? 'bg-blue-600 text-white' : 'bg-blue-50 text-blue-600'}`}>
                            <Icon className="h-5 w-5" />
                        </div>
                        <H2 className={`${featured ? 'text-2xl font-bold' : 'text-lg!'}`}>{title}</H2>
                    </CardHeader>
                )}

                <CardContent className={`flex-1 flex flex-col ${image && !mainImageError ? 'p-6' : 'pt-4'} pb-4`}>
                    <ul className="space-y-3 list-none">
                        {previewItems.map((item, idx) => (
                            <ResourceItem key={idx} item={item} icon={Icon} />
                        ))}
                        {items.length === 0 && (
                            <div className="py-8 text-center bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
                                <p className="text-sm text-slate-400 italic">Curating excellence...</p>
                            </div>
                        )}
                    </ul>

                    {hasMore && (
                        <button
                            onClick={() => setIsModalOpen(true)}
                            className="w-full mt-auto py-2 flex items-center justify-center gap-2 text-xs font-bold text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all border border-slate-100 group/btn"
                        >
                            Open {title} Directory ({items.length} items) <ExternalLink className="h-3 w-3 group-hover/btn:translate-x-0.5 transition-transform" />
                        </button>
                    )}
                </CardContent>
            </Card>

            <ResourceModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title={title}
                items={items}
                icon={Icon}
            />
        </>
    );
};

export const WideResourceCard = ({ icon: Icon, title, items = [], className = "", id, subtitle = "Community Favorites" }: ResourceCardProps & { id?: string }) => {
    const [isModalOpen, setIsModalOpen] = useState(false);

    const previewItems = items.slice(0, 6);
    const hasMore = items.length > 6;

    return (
        <>
            <Card id={id} className={`overflow-hidden group flex flex-col transition-all duration-300 hover:shadow-xl scroll-mt-24 border-slate-200/60 bg-white/50 backdrop-blur-sm ${className}`}>
                <CardHeader className="flex flex-row items-center gap-3 pb-2 border-none">
                    <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 shadow-sm ring-1 ring-indigo-100/50">
                        <Icon className="h-5 w-5" />
                    </div>
                    <div>
                        <H2 className="text-lg! font-bold text-slate-900">{title}</H2>
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-0.5">{subtitle}</p>
                    </div>
                </CardHeader>
                <CardContent className="pt-2 pb-6 flex-1 flex flex-col">
                    <ul className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-2 list-none">
                        {previewItems.map((item, idx) => (
                            <ResourceItem key={idx} item={item} icon={Icon} />
                        ))}
                    </ul>

                    {hasMore && (
                        <button
                            onClick={() => setIsModalOpen(true)}
                            className="w-full mt-auto py-2.5 flex items-center justify-center gap-2 text-xs font-bold text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-xl transition-all border border-slate-100 group/btn bg-white/50"
                        >
                            Open {title} Directory ({items.length} items) <ExternalLink className="h-3 w-3 group-hover/btn:translate-x-0.5 transition-transform" />
                        </button>
                    )}
                </CardContent>
            </Card>

            <ResourceModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title={title}
                items={items}
                icon={Icon}
            />
        </>
    );
};
