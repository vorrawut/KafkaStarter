// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const {themes} = require('prism-react-renderer');
const lightCodeTheme = themes.github;
const darkCodeTheme = themes.dracula;

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Kafka Mastery Curriculum',
  tagline: 'Transform from Kafka beginner to production expert',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'https://vorrawutjudasri.github.io',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/KafkaStarter/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'vorrawutjudasri', // Usually your GitHub org/user name.
  projectName: 'KafkaStarter', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Point to the class directory
          path: '../class',
          routeBasePath: '/', // Serve the docs at the site's root
          editUrl: 'https://github.com/vorrawut/KafkaStarter/tree/main/class/',
        },
        blog: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themes: ['@docusaurus/theme-mermaid'],
  markdown: {
    mermaid: true,
  },

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      // Replace with your project's social card
      image: 'img/kafka-social-card.jpg',
      navbar: {
        title: 'Kafka Mastery',
        logo: {
          alt: 'Kafka Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Curriculum',
          },
          {
            href: 'https://github.com/vorrawut/KafkaStarter',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Learning Path',
            items: [
              {
                label: 'Phase 1: Foundations',
                to: '/modules/lesson_1/concept',
              },
              {
                label: 'Phase 2: Resilient Messaging',
                to: '/modules/lesson_7/concept',
              },
              {
                label: 'Phase 3: Streaming',
                to: '/modules/lesson_14/concept',
              },
              {
                label: 'Phase 4: Production',
                to: '/modules/lesson_18/concept',
              },
            ],
          },
          {
            title: 'Resources',
            items: [
              {
                label: 'Curriculum Guide',
                to: '/CURRICULUM_GUIDE',
              },
              {
                label: 'Quick Start',
                href: 'https://github.com/vorrawut/KafkaStarter/blob/main/QUICK_START_GUIDE.md',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'GitHub',
                href: 'https://github.com/vorrawut/KafkaStarter',
              },
              {
                label: 'Demo System',
                href: 'https://github.com/vorrawut/KafkaStarter/tree/main/demo',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Kafka Mastery Curriculum. Built with Docusaurus.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: ['kotlin', 'yaml', 'bash'],
      },
      mermaid: {
        theme: {light: 'neutral', dark: 'dark'},
      },
    }),
};

module.exports = config;