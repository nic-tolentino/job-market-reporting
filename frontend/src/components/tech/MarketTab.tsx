import { Link } from 'react-router-dom';
import { PieChart, Pie, Cell, Tooltip } from 'recharts';
import { Briefcase } from 'lucide-react';
import { Card, CardHeader, CardContent } from '../ui/Card';
import { H2 } from '../ui/Typography';
import Dropdown from '../ui/Dropdown';
import SimplePager from '../ui/SimplePager';
import CompanyLogo from '../common/CompanyLogo';
import { FeedbackButton } from '../common/Feedback';
import { useChartStyles } from '../../hooks/useChartStyles';
import { type TechDetailsPageDto } from '../../lib/api';
import { JobList } from '../common/JobList';

interface MarketTabProps {
    data: TechDetailsPageDto;
    selectedSeniority: string;
    setSelectedSeniority: (val: string) => void;
    selectedLocation: string;
    setSelectedLocation: (val: string) => void;
    companiesPage: number;
    setCompaniesPage: (val: number) => void;
    jobsPage: number;
    setJobsPage: (val: number) => void;
    seniorityOptions: string[];
    locationOptions: string[];
    paginatedRoles: any[];
    paginatedCompanies: any[];
    totalJobsPages: number;
    totalCompaniesPages: number;
    filteredHiringCompanies: any[];
}

export const MarketTab = ({
    data,
    selectedSeniority,
    setSelectedSeniority,
    selectedLocation,
    setSelectedLocation,
    companiesPage,
    setCompaniesPage,
    jobsPage,
    setJobsPage,
    seniorityOptions,
    locationOptions,
    paginatedRoles,
    paginatedCompanies,
    totalJobsPages,
    totalCompaniesPages,
    filteredHiringCompanies
}: MarketTabProps) => {
    const { tooltipStyle, tooltipItemStyle, pieColors } = useChartStyles();

    return (
        <div className="space-y-8 animate-in fade-in duration-500">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Seniority Donut Chart */}
                <Card>
                    <CardHeader>
                        <H2>Seniority Distribution</H2>
                        <FeedbackButton variant="icon" context={`${data.techName} Seniority Breakdown`} />
                    </CardHeader>
                    <CardContent className="flex-1 flex flex-col items-center justify-center min-h-[400px]">
                        <div className="h-64 flex items-center justify-center">
                            <PieChart width={250} height={250}>
                                <Pie
                                    data={data.seniorityDistribution}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={85}
                                    paddingAngle={5}
                                    dataKey="value"
                                    stroke="none"
                                >
                                    {data.seniorityDistribution.map((_entry: any, index: number) => (
                                        <Cell key={`cell-${index}`} fill={pieColors[index % pieColors.length]} style={{ outline: 'none' }} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    contentStyle={tooltipStyle}
                                    itemStyle={tooltipItemStyle}
                                />
                            </PieChart>
                        </div>
                        <div className="mt-8 grid grid-cols-2 gap-x-6 gap-y-3 text-sm px-2">
                            {data.seniorityDistribution.map((entry: any, index: number) => (
                                <div key={entry.name} className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: pieColors[index % pieColors.length] }}></div>
                                    <span className="text-secondary font-medium whitespace-nowrap">{entry.name}</span>
                                </div>
                            ))}
                        </div>
                    </CardContent>
                </Card>

                {/* Hiring Companies Table */}
                <Card className="lg:col-span-2">
                    <CardHeader className="flex flex-row items-center justify-between">
                        <H2>Actively Hiring Companies</H2>
                        <div className="flex items-center gap-4">
                            <SimplePager
                                currentPage={companiesPage}
                                totalPages={totalCompaniesPages}
                                onPageChange={setCompaniesPage}
                            />
                            <FeedbackButton variant="icon" context={`${data.techName} Hiring Companies`} />
                        </div>
                    </CardHeader>
                    <div className="flex-1 overflow-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="table-header-row bg-elevated text-secondary sticky top-0 z-20">
                                <tr>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider text-right">Active {data.techName} Roles</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-border-subtle">
                                {paginatedCompanies.map((company: any) => (
                                    <tr key={company.id} className="hover:bg-surface-hover transition-colors group">
                                        <td className="px-6 py-4">
                                            <Link to={"/company/" + company.id} className="flex items-center gap-3">
                                                <CompanyLogo
                                                    logoUrl={company.logo}
                                                    companyName={company.name}
                                                    className="h-10 w-10 rounded-lg border border-border shadow-theme-sm group-hover:border-accent group-hover:text-accent transition-colors"
                                                />
                                                <span className="font-semibold text-primary group-hover:text-accent transition-colors flex-1 truncate" title={company.name}>
                                                    {company.name}
                                                </span>
                                            </Link>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <div className="inline-flex items-center gap-1.5 rounded-full bg-accent-subtle px-2.5 py-1 text-sm font-semibold text-accent">
                                                <Briefcase className="h-4 w-4" />
                                                {company.activeRoles}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                {filteredHiringCompanies.length === 0 && (
                                    <tr>
                                        <td colSpan={2} className="px-6 py-12 text-center text-muted italic">
                                            No companies found for this filter combination.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </Card>
            </div>

            {/* Job Listings Section */}
            <div className="space-y-6">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <H2>Active {data.techName} Roles</H2>

                    <div className="flex flex-wrap items-center gap-3">
                        <Dropdown
                            value={selectedSeniority}
                            onChange={setSelectedSeniority}
                            options={[
                                { value: 'All', label: 'All Seniorities' },
                                ...seniorityOptions.map((opt: string) => ({ value: opt, label: opt }))
                            ]}
                            labelPrefix="Seniority"
                        />

                        <Dropdown
                            value={selectedLocation}
                            onChange={setSelectedLocation}
                            options={[
                                { value: 'All', label: 'All Locations' },
                                ...locationOptions.map((loc: string) => ({ value: loc, label: loc }))
                            ]}
                            labelPrefix="Location"
                        />
                    </div>
                </div>

                <JobList 
                    jobs={paginatedRoles}
                    title={`Active ${data.techName} Roles`}
                    page={jobsPage}
                    setPage={setJobsPage}
                    totalPages={totalJobsPages}
                    context={`${data.techName} Job Listings`}
                    showPagination={true}
                />

            </div>
        </div>
    );
};
