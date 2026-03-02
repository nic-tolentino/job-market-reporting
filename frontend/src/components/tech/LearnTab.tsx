import { Youtube, BookOpen, Radio, Globe, Code, Users } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { ResourceCard } from './ResourceCard';
import { LEARN_RESOURCES } from '../../constants/techResources';

interface LearnTabProps {
    techId: string;
    techName: string;
}

export const LearnTab = ({ techId, techName }: LearnTabProps) => {
    const resources = LEARN_RESOURCES[techId.toLowerCase()];

    if (!resources) {
        return (
            <div className="py-20 text-center animate-in fade-in duration-500">
                <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                    <BookOpen className="h-8 w-8 text-gray-400" />
                </div>
                <H2>Learning Resources Coming Soon</H2>
                <p className="text-gray-500 mt-2">We're currently curating the best learning materials for {techName}.</p>
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in fade-in duration-500">
            <ResourceCard icon={Youtube} title="YouTube Channels" items={resources.youtube} />
            <ResourceCard icon={BookOpen} title="Courses" items={resources.courses} />
            <ResourceCard icon={Radio} title="Podcasts" items={resources.podcasts} />
            <ResourceCard icon={Globe} title="Websites & Blogs" items={resources.websites} />
            <ResourceCard icon={Code} title="Open Source Projects" items={resources.projects} />
            <ResourceCard icon={Users} title="People to Follow" items={resources.people} />
        </div>
    );
};
