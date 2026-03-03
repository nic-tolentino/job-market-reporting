import { Youtube, BookOpen, Radio, Globe, Code, Hash } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { ResourceCard, WideResourceCard } from './ResourceCard';
import { LEARN_RESOURCES } from '../../constants/techResources';
import { SpotlightSection } from './SpotlightSection';

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
        <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-700">
            {/* Quick Links */}
            <div className="flex flex-wrap gap-2 pb-2">
                {['Courses', 'YouTube', 'Podcasts', 'Websites', 'Communities', 'Projects', 'Experts'].map((item) => (
                    <a
                        key={item}
                        href={`#${item.toLowerCase().replace(' ', '-')}`}
                        className="px-3 py-1.5 bg-white border border-slate-200 rounded-full text-[10px] font-bold text-slate-500 hover:bg-blue-50 hover:text-blue-600 hover:border-blue-100 transition-all shadow-sm"
                    >
                        {item}
                    </a>
                ))}
            </div>

            {/* Featured Section */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <ResourceCard
                    id="courses"
                    icon={BookOpen}
                    title="Featured Courses"
                    items={resources.courses}
                    featured
                    image={techImage}
                />
                <ResourceCard
                    id="youtube"
                    icon={Youtube}
                    title="Top Channels"
                    items={resources.youtube}
                />
            </div>

            {/* Wide Projects Section */}
            <WideResourceCard id="projects" icon={Code} title="Global Open Source Projects" items={resources.projects} subtitle="Global Standards" className="shadow-md border-indigo-50" />

            {/* Secondary Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <ResourceCard id="podcasts" icon={Radio} title="Tech Podcasts" items={resources.podcasts} />
                <ResourceCard id="websites" icon={Globe} title="Reference Sites" items={resources.websites} />
                <ResourceCard id="communities" icon={Hash} title="Global Communities" items={resources.communities} />
            </div>

            {/* Spotlight Section */}
            <SpotlightSection
                id="experts"
                title="Industry Experts & Influencers"
                items={resources.people}
            />
        </div>
    );
};
