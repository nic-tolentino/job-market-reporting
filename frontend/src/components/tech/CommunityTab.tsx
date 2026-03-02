import { Users, Calendar, Terminal } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { Card } from '../ui/Card';
import { ResourceCard } from './ResourceCard';
import { COMMUNITY_RESOURCES } from '../../constants/techResources';

interface CommunityTabProps {
    techId: string;
    techName: string;
}

export const CommunityTab = ({ techId, techName }: CommunityTabProps) => {
    const resources = COMMUNITY_RESOURCES[techId.toLowerCase()];

    if (!resources) {
        return (
            <div className="py-20 text-center animate-in fade-in duration-500">
                <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                    <Users className="h-8 w-8 text-gray-400" />
                </div>
                <H2>Community Hub Coming Soon</H2>
                <p className="text-gray-500 mt-2">We're gathering local meetups and community info for {techName}.</p>
            </div>
        );
    }

    return (
        <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
            {/* Community Hero */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <ResourceCard
                    icon={Calendar}
                    title="Upcoming NZ Events"
                    items={resources.events}
                    featured
                    className="bg-blue-600 text-white"
                />
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <ResourceCard icon={Users} title="Local Meetups" items={resources.meetups} />
                    <ResourceCard icon={Terminal} title="Open Source" items={resources.localProjects} />
                </div>
            </div>

            {/* Celebrities / People Section */}
            <div className="pt-4 border-t border-gray-100">
                <H2 className="mb-6">Community Spotlight</H2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {resources.celebrities.map((person, idx) => (
                        <Card key={idx} className="p-4 hover:shadow-md transition-all group">
                            <div className="flex items-center gap-3">
                                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center text-white font-bold text-xl">
                                    {person.title.charAt(0)}
                                </div>
                                <div>
                                    <h4 className="font-bold text-slate-900 group-hover:text-blue-600 transition-colors">
                                        <a href={person.url} target="_blank" rel="noopener noreferrer">{person.title}</a>
                                    </h4>
                                    <p className="text-xs text-gray-500">{person.description}</p>
                                </div>
                            </div>
                        </Card>
                    ))}
                </div>
            </div>
        </div>
    );
};
