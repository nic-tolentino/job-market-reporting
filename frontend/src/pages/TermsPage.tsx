import { Card } from '../components/ui/Card';
import { H2 } from '../components/ui/Typography';

export default function TermsPage() {
    return (
        <div className="max-w-4xl mx-auto space-y-10 py-8 md:py-12">
            <section className="text-center px-4">
                <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight text-slate-900 leading-tight">
                    Terms & Conditions
                </h1>
                <p className="mx-auto mt-4 max-w-2xl text-lg text-gray-600">
                    Effective Date: February 27, 2026
                </p>
            </section>

            <Card className="p-0 overflow-hidden">
                <div className="p-6 md:p-8 prose prose-slate max-w-none text-gray-600 space-y-6">
                    <div>
                        <H2 className="text-slate-900 mb-2">1. Description of Service</H2>
                        <p>
                            TechMarket is a personal, non-profit, public-interest project. We aggregate publicly available job listings from third-party sources to provide a centralized search utility. We are <strong>not</strong> a recruitment agency or employer.
                        </p>
                    </div>

                    <div>
                        <H2 className="text-slate-900 mb-2">2. Use of the Website</H2>
                        <p>
                            You are granted a limited license for personal, non-commercial use. Automated scraping of our aggregated data for commercial purposes is prohibited.
                        </p>
                    </div>

                    <div>
                        <H2 className="text-slate-900 mb-2">3. Disclaimers ("As-Is")</H2>
                        <p>
                            The Website is provided on an "AS IS" basis. We make no warranties regarding the accuracy or timeliness of job listings, which may be modified or removed by the original source at any time. Salary bands and technologies are often inferred through automated processing; please verify all info directly with employers.
                        </p>
                    </div>

                    <div>
                        <H2 className="text-slate-900 mb-2">4. Limitation of Liability</H2>
                        <p>
                            To the maximum extent permitted by NZ and AU law, the Project and its developer shall not be liable for any damages arising out of your use of the Website.
                        </p>
                    </div>

                    <div>
                        <H2 className="text-slate-900 mb-2">5. Governing Law</H2>
                        <p>These terms are governed by the laws of New Zealand.</p>
                    </div>

                    <div className="pt-6 border-t border-gray-100 italic text-sm">
                        For any questions, please contact Nic Tolentino at hello@nictolentino.com
                    </div>
                </div>
            </Card>
        </div>
    );
}
