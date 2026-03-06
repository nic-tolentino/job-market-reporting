import { Youtube, BookOpen, Radio, Globe, Code, Hash } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { ResourceCard, WideResourceCard } from './ResourceCard';
import { LEARN_RESOURCES } from '../../constants/techResources';
import { SpotlightSection } from './SpotlightSection';
import { SubmitResourceModal } from '../common/SubmitResourceModal';
import { useState, useMemo } from 'react';
import { useAppStore } from '../../store/useAppStore';

interface LearnTabProps {
    techId: string;
    techName: string;
}

export const LearnTab = ({ techId, techName }: LearnTabProps) => {
    const [isSubmitModalOpen, setIsSubmitModalOpen] = useState(false);
    const { selectedCountry } = useAppStore();
    const rawResources = LEARN_RESOURCES[techId.toLowerCase()];

    const resources = useMemo(() => {
        if (!rawResources) return null;
        const filterFn = (items: any[]) => items.filter(item => 
            !item.countries || 
            item.countries.includes(selectedCountry) || 
            item.countries.includes('Global')
        );

        return {
            youtube: filterFn(rawResources.youtube),
            courses: filterFn(rawResources.courses),
            podcasts: filterFn(rawResources.podcasts),
            websites: filterFn(rawResources.websites),
            projects: filterFn(rawResources.projects),
            people: filterFn(rawResources.people),
            communities: filterFn(rawResources.communities),
        };
    }, [rawResources, selectedCountry]);

    const isAndroid = techId.toLowerCase() === 'android';
    const techImage = isAndroid
        ? '/assets/android-feature.png'
        : '/assets/ios-feature.png';

    if (!resources) {
        return (
            <div className="py-20 text-center animate-in fade-in duration-500">
                <div className="mx-auto w-16 h-16 bg-inset rounded-full flex items-center justify-center mb-4">
                    <BookOpen className="h-8 w-8 text-muted" />
                </div>
                <H2>Learning Resources Coming Soon</H2>
                <p className="text-muted mt-2">We're currently curating the best learning materials for {techName}.</p>
            </div>
        );
    }

    return (
        <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-700">


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
            <WideResourceCard id="projects" icon={Code} title="Global Open Source Projects" items={resources.projects} subtitle="Global Standards" className="shadow-md border-transparent dark:border-[#1E293B]" />

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
            {/* Bottom: Submit Resource CTA */}
            <div className="banner-cta rounded-3xl p-8 border flex flex-col md:flex-row items-center justify-between gap-6 shadow-theme-sm">
                <div className="max-w-md text-center md:text-left">
                    <h3 className="text-xl font-bold text-primary">Know a great learning resource?</h3>
                    <p className="text-muted mt-1 text-sm">Help us grow the most complete directory of developer learning material! Suggest an amazing YouTube channel, a helpful podcast, or a killer guide for {techName}.</p>
                </div>
                <button
                    onClick={() => setIsSubmitModalOpen(true)}
                    className="bg-card text-accent px-6 py-3 rounded-2xl font-bold shadow-[0_4px_14px_rgba(0,0,0,0.05)] hover:shadow-[0_4px_20px_rgba(59,130,246,0.15)] hover:scale-[1.02] transition-all border border-accent-subtle whitespace-nowrap active:scale-95"
                >
                    Submit a Resource
                </button>
            </div>

            <SubmitResourceModal
                isOpen={isSubmitModalOpen}
                onClose={() => setIsSubmitModalOpen(false)}
                techName={techName}
                contextType="Learning"
            />
        </div>
    );
};

