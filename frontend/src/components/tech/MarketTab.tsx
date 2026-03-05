import { Link, useNavigate } from 'react-router-dom';
import { PieChart, Pie, Cell, Tooltip } from 'recharts';
import { Briefcase, MapPin, DollarSign, Calendar } from 'lucide-react';
import { Card, CardHeader, CardContent } from '../ui/Card';
import { H2 } from '../ui/Typography';
import Dropdown from '../ui/Dropdown';
import SimplePager from '../ui/SimplePager';
import CompanyLogo from '../common/CompanyLogo';
import { FeedbackButton } from '../common/Feedback';
import { useChartStyles } from '../../hooks/useChartStyles';
import { type TechDetailsPageDto } from '../../lib/api';

const JOBS_PAGE_SIZE = 10;

interface MarketTabProps {
    data: TechDetailsPageDto;
    selectedSeniority: string;
    setSelectedSeniority: (val: string) => void;
    selectedCity: string;
    setSelectedCity: (val: string) => void;
    companiesPage: number;
    setCompaniesPage: (val: number) => void;
    jobsPage: number;
    setJobsPage: (val: number) => void;
    seniorityOptions: string[];
    cityOptions: string[];
    filteredRoles: any[];
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
    selectedCity,
    setSelectedCity,
    companiesPage,
    setCompaniesPage,
    jobsPage,
    setJobsPage,
    seniorityOptions,
    cityOptions,
    filteredRoles,
    paginatedRoles,
    paginatedCompanies,
    totalJobsPages,
    totalCompaniesPages,
    filteredHiringCompanies
}: MarketTabProps) => {
    const navigate = useNavigate();
    const { tooltipStyle, pieColors } = useChartStyles();

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
                                    {data.seniorityDistribution.map((_entry, index) => (
                                        <Cell key={`cell-${index}`} fill={pieColors[index % pieColors.length]} style={{ outline: 'none' }} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    contentStyle={tooltipStyle}
                                />
                            </PieChart>
                        </div>
                        <div className="mt-8 grid grid-cols-2 gap-x-6 gap-y-3 text-sm px-2">
                            {data.seniorityDistribution.map((entry, index) => (
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
                            <thead className="bg-elevated text-muted sticky top-0">
                                <tr>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider text-right">Active {data.techName} Roles</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-border-subtle">
                                {paginatedCompanies.map((company) => (
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
                                ...seniorityOptions.map(opt => ({ value: opt, label: opt }))
                            ]}
                            labelPrefix="Seniority"
                        />

                        <Dropdown
                            value={selectedCity}
                            onChange={setSelectedCity}
                            options={[
                                { value: 'All', label: 'All Cities' },
                                ...cityOptions.map(city => ({ value: city, label: city }))
                            ]}
                            labelPrefix="Location"
                        />
                    </div>
                </div>

                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <div className="text-sm font-medium text-muted">
                            Showing <span className="text-primary font-bold">{filteredRoles.length > 0 ? (jobsPage - 1) * JOBS_PAGE_SIZE + 1 : 0}</span> to <span className="text-primary font-bold">{Math.min(jobsPage * JOBS_PAGE_SIZE, filteredRoles.length)}</span> of <span className="text-primary font-bold">{filteredRoles.length}</span> positions
                        </div>
                        <div className="flex items-center gap-4">
                            <SimplePager
                                currentPage={jobsPage}
                                totalPages={totalJobsPages}
                                onPageChange={setJobsPage}
                            />
                            <FeedbackButton variant="icon" context={`${data.techName} Job Listings`} />
                        </div>
                    </CardHeader>
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-elevated text-muted">
                                <tr>
                                    <th className="px-6 py-4 font-medium uppercase tracking-wider">Role</th>
                                    <th className="px-6 py-4 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-4 font-medium uppercase tracking-wider">Compensation</th>
                                    <th className="px-6 py-4 font-medium uppercase tracking-wider">Posted</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-border-subtle">
                                {filteredRoles.length === 0 ? (
                                    <tr>
                                        <td colSpan={4} className="px-6 py-12 text-center text-muted">
                                            No {data.techName} roles found matching your criteria.
                                        </td>
                                    </tr>
                                ) : (
                                    paginatedRoles.map((role) => (
                                        <tr
                                            key={role.id}
                                            onClick={() => navigate(`/job/${role.id}`)}
                                            className="hover:bg-surface-hover transition-colors cursor-pointer group/row"
                                        >
                                            <td className="px-6 py-5">
                                                <span className="font-semibold text-primary group-hover/row:text-accent transition-colors">
                                                    {role.title}
                                                </span>
                                                <div className="mt-1 flex items-center gap-1.5 text-muted">
                                                    <MapPin className="h-3.5 w-3.5" />
                                                    <span>{role.locations.join(' · ')}</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-5">
                                                <div className="flex items-center gap-2">
                                                    <span className="font-medium text-secondary">{role.companyName}</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-5">
                                                {role.salaryMin && role.salaryMax ? (
                                                    <div className="flex items-center gap-1 font-medium text-secondary">
                                                        <DollarSign className="h-4 w-4 text-muted" />
                                                        ${(role.salaryMin / 1000)}k - ${(role.salaryMax / 1000)}k
                                                    </div>
                                                ) : (
                                                    <span className="text-muted italic">Unlisted</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-5 text-muted whitespace-nowrap">
                                                <div className="flex items-center gap-1.5">
                                                    <Calendar className="h-4 w-4" />
                                                    {role.postedDate}
                                                </div>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                    {totalJobsPages > 1 && (
                        <div className="flex justify-center border-t border-border-subtle p-4">
                            <SimplePager
                                currentPage={jobsPage}
                                totalPages={totalJobsPages}
                                onPageChange={setJobsPage}
                            />
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
};
