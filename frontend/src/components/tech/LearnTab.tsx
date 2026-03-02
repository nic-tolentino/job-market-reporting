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
    const isAndroid = techId.toLowerCase() === 'android';
    const techImage = isAndroid
        ? '/assets/android-feature.png'
        : '/assets/ios-feature.png';

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
        <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
            {/* Featured Section */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <ResourceCard
                    icon={BookOpen}
                    title="Featured Courses"
                    items={resources.courses}
                    featured
                    image={techImage}
                />
                <ResourceCard
                    icon={Youtube}
                    title="Top Channels"
                    items={resources.youtube}
                />
            </div>

            {/* Secondary Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <ResourceCard icon={Radio} title="Podcasts" items={resources.podcasts} />
                <ResourceCard icon={Globe} title="Websites" items={resources.websites} />
                <ResourceCard icon={Code} title="Projects" items={resources.projects} />
                <ResourceCard icon={Users} title="People" items={resources.people} />
            </div>
        </div>
    );
};
