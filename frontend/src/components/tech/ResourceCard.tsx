import { ExternalLink } from 'lucide-react';
import { Card, CardHeader, CardContent } from '../ui/Card';
import { H2 } from '../ui/Typography';
import { type Resource } from '../../constants/techResources';

interface ResourceCardProps {
    icon: any;
    title: string;
    items?: Resource[];
    image?: string;
    featured?: boolean;
    className?: string;
}

export const ResourceCard = ({ icon: Icon, title, items = [], image, featured, className = "" }: ResourceCardProps) => {
    return (
        <Card className={`overflow-hidden group h-full transition-all duration-300 hover:shadow-xl hover:-translate-y-1 ${featured ? 'md:col-span-2 bg-gradient-to-br from-blue-50 to-indigo-50 border-blue-100' : ''} ${className}`}>
            {image && (
                <div className="h-48 overflow-hidden relative">
                    <img src={image} alt={title} className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" />
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

            {!image && (
                <CardHeader className={`flex flex-row items-center gap-3 pb-2 ${featured ? 'pt-6 px-6' : ''}`}>
                    <div className={`p-2 rounded-lg ${featured ? 'bg-blue-600 text-white' : 'bg-blue-50 text-blue-600'}`}>
                        <Icon className="h-5 w-5" />
                    </div>
                    <H2 className={`${featured ? 'text-2xl font-bold' : 'text-lg!'}`}>{title}</H2>
                </CardHeader>
            )}

            <CardContent className={`${image ? 'p-6' : 'pt-4'} pb-6`}>
                <ul className="space-y-3">
                    {items.map((item, idx) => (
                        <li key={idx} className="group/item">
                            <a
                                href={item.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex items-center gap-4 hover:bg-white/60 -mx-3 px-3 py-2.5 rounded-xl transition-all border border-transparent hover:border-blue-100 hover:shadow-sm"
                            >
                                {item.previewImage && (
                                    <div className="w-14 h-11 rounded-lg bg-gray-50 overflow-hidden flex-shrink-0 border border-slate-100 shadow-sm relative">
                                        <img src={item.previewImage} alt="" className="w-full h-full object-cover group-hover/item:scale-110 transition-transform duration-500" />
                                        <div className="absolute inset-0 bg-black/5 opacity-0 group-hover/item:opacity-100 transition-opacity" />
                                    </div>
                                )}
                                {!item.previewImage && (
                                    <div className="w-10 h-10 rounded-lg bg-slate-50 flex items-center justify-center flex-shrink-0 text-slate-400 group-hover/item:bg-blue-50 group-hover/item:text-blue-500 transition-colors">
                                        <Icon className="h-5 w-5 opacity-60" />
                                    </div>
                                )}
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center justify-between gap-2">
                                        <span className="font-semibold text-slate-900 group-hover/item:text-blue-600 transition-colors truncate">
                                            {item.title}
                                        </span>
                                        <ExternalLink className="h-3 w-3 text-slate-300 opacity-0 group-hover/item:opacity-100 transition-all -translate-x-1 group-hover/item:translate-x-0" />
                                    </div>
                                    <p className="text-xs text-slate-500 mt-0.5 line-clamp-1 group-hover/item:text-slate-600">{item.description}</p>
                                </div>
                            </a>
                        </li>
                    ))}
                    {items.length === 0 && (
                        <div className="py-8 text-center bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
                            <p className="text-sm text-slate-400 italic">Curating excellence...</p>
                        </div>
                    )}
                </ul>
            </CardContent>
        </Card>
    );
};
