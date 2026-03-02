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
                <ul className="space-y-4">
                    {items.map((item, idx) => (
                        <li key={idx} className="group/item">
                            <a
                                href={item.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="block hover:bg-white/50 -mx-2 px-2 py-2 rounded-lg transition-colors border border-transparent hover:border-blue-100"
                            >
                                <div className="flex items-center justify-between">
                                    <span className="font-semibold text-slate-900 group-hover/item:text-blue-600 transition-colors flex items-center gap-2">
                                        {item.title}
                                        <ExternalLink className="h-3 w-3 opacity-0 group-hover/item:opacity-100 transition-opacity" />
                                    </span>
                                </div>
                                <p className="text-sm text-gray-500 mt-1 line-clamp-2">{item.description}</p>
                            </a>
                        </li>
                    ))}
                    {items.length === 0 && (
                        <p className="text-sm text-gray-400 italic">Coming soon...</p>
                    )}
                </ul>
            </CardContent>
        </Card>
    );
};
