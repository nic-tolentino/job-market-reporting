import { ExternalLink } from 'lucide-react';
import { Card, CardHeader, CardContent } from '../ui/Card';
import { H2 } from '../ui/Typography';
import { type Resource } from '../../constants/techResources';

interface ResourceCardProps {
    icon: any;
    title: string;
    items: Resource[];
}

export const ResourceCard = ({ icon: Icon, title, items }: ResourceCardProps) => (
    <Card className="h-full">
        <CardHeader className="border-b border-gray-50 pb-4">
            <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-blue-50 text-blue-600">
                    <Icon className="h-5 w-5" />
                </div>
                <H2 className="text-lg!">{title}</H2>
            </div>
        </CardHeader>
        <CardContent className="pt-4">
            <ul className="space-y-4">
                {items.map((item, idx) => (
                    <li key={idx} className="group">
                        <a
                            href={item.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="block hover:bg-gray-50 -mx-2 px-2 py-2 rounded-lg transition-colors"
                        >
                            <div className="flex items-start justify-between">
                                <div className="font-semibold text-slate-900 group-hover:text-blue-600 transition-colors flex items-center gap-1.5">
                                    {item.name}
                                    <ExternalLink className="h-3 w-3 opacity-0 group-hover:opacity-100 transition-opacity" />
                                </div>
                            </div>
                            <p className="text-sm text-gray-500 mt-0.5 line-clamp-2">{item.description}</p>
                        </a>
                    </li>
                ))}
            </ul>
        </CardContent>
    </Card>
);
