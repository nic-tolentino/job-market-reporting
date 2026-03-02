import { Users, Calendar, Terminal } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { Card } from '../ui/Card';
import { ResourceCard } from './ResourceCard';
import { COMMUNITY_RESOURCES } from '../../constants/techResources';
import type { Resource } from '../../constants/techResources';
import { useState } from 'react';

const SpotlightCard = ({ person }: { person: Resource }) => {
    const [imageError, setImageError] = useState(false);
    const initials = person.title.split(' ').map(n => n[0]).join('').slice(0, 2);

    return (
        <Card className="p-4 hover:shadow-xl hover:-translate-y-1 transition-all group bg-white border-slate-100/60 ring-1 ring-black/5">
            <div className="flex flex-col items-center text-center gap-3">
                <div className="relative">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-400 rotate-3 group-hover:rotate-6 transition-transform flex items-center justify-center text-white font-black text-2xl shadow-lg shadow-blue-200/50 overflow-hidden">
                        {person.previewImage && !imageError ? (
                            <img
                                src={person.previewImage}
                                alt=""
                                className="w-full h-full object-cover -rotate-3 group-hover:-rotate-6 transition-transform scale-110"
                                onError={() => setImageError(true)}
                            />
                        ) : (
                            <span>{initials}</span>
                        )}
                    </div>
                    <div className="absolute -bottom-1 -right-1 w-6 h-6 bg-white rounded-full flex items-center justify-center shadow-sm">
                        <div className="w-4 h-4 rounded-full bg-green-500 border-2 border-white"></div>
                    </div>
                </div>
                <div className="w-full px-1">
                    <h4 className="font-bold text-slate-900 group-hover:text-blue-600 transition-colors text-sm md:text-base truncate">
                        <a href={person.url} target="_blank" rel="noopener noreferrer">{person.title}</a>
                    </h4>
                    <div className="flex flex-col gap-0.5 mt-1">
                        <p className="text-[10px] uppercase font-bold tracking-wider text-slate-400 line-clamp-1">{person.description.split(',')[0] || 'NZ Developer'}</p>
                        {person.location && (
                            <p className="text-[9px] font-medium text-blue-500/70">{person.location}</p>
                        )}
                    </div>
                </div>
            </div>
        </Card>
    );
};

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
        <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-700">
            {/* Top Row: Hero Event + Key Communities */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
                <div className="lg:col-span-7 h-full">
                    <ResourceCard
                        icon={Calendar}
                        title="Major Community Events"
                        items={resources.events}
                        featured
                        className="bg-slate-900 border-none relative overflow-hidden h-full"
                    />
                </div>
                <div className="lg:col-span-5 flex flex-col gap-6">
                    <ResourceCard icon={Users} title="Active Meetups" items={resources.meetups} className="bg-white" />
                    <ResourceCard icon={Terminal} title="NZ Open Source" items={resources.localProjects} className="bg-white border-blue-50" />
                </div>
            </div>

            {/* Middle Section: Community Spotlight Gallery */}
            <div className="relative">
                <div className="absolute inset-0 flex items-center" aria-hidden="true">
                    <div className="w-full border-t border-slate-100"></div>
                </div>
                <div className="relative flex justify-center">
                    <span className="px-6 bg-slate-50 text-xl font-bold text-slate-800 tracking-tight">Community Spotlight</span>
                </div>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 px-2">
                {resources.celebrities.map((person, idx) => (
                    <SpotlightCard key={idx} person={person} />
                ))}
            </div>

            {/* Bottom: Regional Connectivity */}
            <div className="bg-gradient-to-r from-blue-50/50 to-indigo-50/50 rounded-3xl p-8 border border-white flex flex-col md:flex-row items-center justify-between gap-6 shadow-sm">
                <div className="max-w-md text-center md:text-left">
                    <h3 className="text-xl font-bold text-slate-800">Missed a local group?</h3>
                    <p className="text-slate-500 mt-1 text-sm">We're constantly expanding our NZ tech directory. If you know a community in Wellington, Christchurch or Dunedin, let us know!</p>
                </div>
                <button className="bg-white text-blue-600 px-6 py-3 rounded-2xl font-bold shadow-sm hover:shadow-md transition-all border border-blue-50 whitespace-nowrap">
                    Suggest a Resource
                </button>
            </div>
        </div>
    );
};
