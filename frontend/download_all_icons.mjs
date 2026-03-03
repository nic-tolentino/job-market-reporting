import fs from 'fs';
import https from 'https';

const manualOverrides = {
    'aws': 'amazonaws',
    'gcp': 'googlecloud',
    'azure': 'microsoftazure',
    'dotnet': 'dotnet',
    '.net': 'dotnet',
    'sql-server': 'microsoftsqlserver',
    'sql server': 'microsoftsqlserver',
    'k8s': 'kubernetes',
    'html': 'html5',
    'css': 'css3',
    'sql': 'mysql',
    'mongo': 'mongodb',
    'react native': 'react',
    'node': 'nodedotjs',
    'nodejs': 'nodedotjs',
    'node.js': 'nodedotjs',
    'vue': 'vuedotjs',
    'nextjs': 'nextdotjs',
    'next.js': 'nextdotjs',
    'backbone': 'backbonedotjs',
    'ember': 'emberdotjs',
    'c++': 'cplusplus',
    'c#': 'csharp',
    'mac': 'apple',
    'ios': 'ios',
    'ubuntu': 'ubuntu',
    'kubernetes': 'kubernetes',
    'postgres': 'postgresql',
    'mariadb': 'mariadb',
    'elasticsearch': 'elasticsearch',
    'dynamodb': 'amazondynamodb',
    'cassandra': 'apachecassandra',
    'neo4j': 'neo4j',
    'couchbase': 'couchbase',
    'hadoop': 'apachehadoop',
    'spark': 'apachespark',
    'kafka': 'apachekafka',
    'rabbitmq': 'rabbitmq',
    'activemq': 'apacheactivemq',
    'airflow': 'apacheairflow',
    'databricks': 'databricks',
    'scikit-learn': 'scikitlearn',
    'bigquery': 'googlebigquery',
    'redshift': 'amazonredshift',
    'github actions': 'githubactions',
    'gitlab ci': 'gitlab',
    'travis ci': 'travisci',
    'circleci': 'circleci',
    'terraform': 'terraform',
    'ansible': 'ansible',
    'chef': 'chef',
    'puppet': 'puppet',
    'jenkins': 'jenkins',
    'docker': 'docker',
    'linux': 'linux',
    'serverless': 'serverless',
    'lambda': 'awslambda',
    'cloudformation': 'awscloudformation',
    'java': 'openjdk',
    'angular': 'angular',
    'spring': 'spring',
    'spring boot': 'springboot',
    'django': 'django',
    'flask': 'flask',
    'fastapi': 'fastapi',
    'express': 'express',
    'nestjs': 'nestjs',
    'ruby on rails': 'rubyonrails',
    'laravel': 'laravel',
    'asp.net': 'dotnet',
    'flutter': 'flutter',
    'xamarin': 'xamarin',
    'ionic': 'ionic',
    'kotlin multiplatform': 'kotlin',
    'sqlite': 'sqlite',
    'snowflake': 'snowflake',
    'dbt': 'dbt',
    'pandas': 'pandas',
    'numpy': 'numpy',
    'tensorflow': 'tensorflow',
    'pytorch': 'pytorch',
    'mongodb': 'mongodb',
    'mysql': 'mysql',
    'postgresql': 'postgresql',
    'react': 'react',
    'typescript': 'typescript',
    'javascript': 'javascript',
    'kotlin': 'kotlin',
    'android': 'android'
};

const techMapFile = fs.readFileSync('../backend/src/main/kotlin/com/techmarket/util/TechFormatter.kt', 'utf-8');

let match;
const regex = /"([^"]+)" to "([^"]+)"/g;

const mappedTechs = new Set();
const promises = [];

function downloadFile(url, dest) {
    return new Promise((resolve, reject) => {
        const file = fs.createWriteStream(dest);
        https.get(url, response => {
            if (response.statusCode !== 200) {
                fs.unlink(dest, () => { });
                resolve(false);
            } else {
                response.pipe(file);
                file.on('finish', () => resolve(true));
            }
        }).on('error', err => {
            fs.unlink(dest, () => { });
            resolve(false);
        });
    });
}

const lines = techMapFile.split("\n");
const list = [];
for (let line of lines) {
    const m = regex.exec(line);
    if (m) list.push(m[1].toLowerCase());
}

async function main() {
    for (let key of list) {
        let slug = key.replace(/[^a-z0-9]/g, '');
        if (manualOverrides[key]) {
            slug = manualOverrides[key];
        }

        const safeKey = key.replace(/[^a-z0-9-]/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '');
        const path = `/Users/nic/Projects/job-market-reporting/frontend/public/icons/tech/${safeKey}.svg`;

        if (!fs.existsSync(path)) {
            const success = await downloadFile(`https://cdn.jsdelivr.net/npm/simple-icons@11/icons/${slug}.svg`, path);
            if (success) {
                console.log(`Downloaded ${safeKey}.svg (${slug})`);
            } else {
                console.log(`Failed for ${key} (${slug})`);
            }
        }
    }
}
main();
