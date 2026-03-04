import { Youtube, BookOpen, Radio, Globe, Code, Hash } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { ResourceCard, WideResourceCard } from './ResourceCard';
import { LEARN_RESOURCES } from '../../constants/techResources';
import { SpotlightSection } from './SpotlightSection';
import { SubmitResourceModal } from '../common/SubmitResourceModal';
import { useState } from 'react';

interface LearnTabProps {
    techId: string;
    techName: string;
}

export const LearnTab = ({ techId, techName }: LearnTabProps) => {
    const [isSubmitModalOpen, setIsSubmitModalOpen] = useState(false);
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
            {/* Bottom: Submit Resource CTA */}
            <div className="bg-gradient-to-r from-blue-50/50 to-indigo-50/50 rounded-3xl p-8 border border-white flex flex-col md:flex-row items-center justify-between gap-6 shadow-sm">
                <div className="max-w-md text-center md:text-left">
                    <h3 className="text-xl font-bold text-slate-800">Know a great learning resource?</h3>
                    <p className="text-slate-500 mt-1 text-sm">Help us grow the most complete directory of developer learning material! Suggest an amazing YouTube channel, a helpful podcast, or a killer guide for {techName}.</p>
                </div>
                <button
                    onClick={() => setIsSubmitModalOpen(true)}
                    className="bg-white text-blue-600 px-6 py-3 rounded-2xl font-bold shadow-[0_4px_14px_rgba(0,0,0,0.05)] hover:shadow-[0_4px_20px_rgba(59,130,246,0.15)] hover:scale-[1.02] transition-all border border-blue-50 whitespace-nowrap active:scale-95"
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

