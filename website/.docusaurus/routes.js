import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/KafkaStarter/__docusaurus/debug',
    component: ComponentCreator('/KafkaStarter/__docusaurus/debug', '51c'),
    exact: true
  },
  {
    path: '/KafkaStarter/__docusaurus/debug/config',
    component: ComponentCreator('/KafkaStarter/__docusaurus/debug/config', '254'),
    exact: true
  },
  {
    path: '/KafkaStarter/__docusaurus/debug/content',
    component: ComponentCreator('/KafkaStarter/__docusaurus/debug/content', '1f5'),
    exact: true
  },
  {
    path: '/KafkaStarter/__docusaurus/debug/globalData',
    component: ComponentCreator('/KafkaStarter/__docusaurus/debug/globalData', '11f'),
    exact: true
  },
  {
    path: '/KafkaStarter/__docusaurus/debug/metadata',
    component: ComponentCreator('/KafkaStarter/__docusaurus/debug/metadata', '32c'),
    exact: true
  },
  {
    path: '/KafkaStarter/__docusaurus/debug/registry',
    component: ComponentCreator('/KafkaStarter/__docusaurus/debug/registry', '722'),
    exact: true
  },
  {
    path: '/KafkaStarter/__docusaurus/debug/routes',
    component: ComponentCreator('/KafkaStarter/__docusaurus/debug/routes', '99d'),
    exact: true
  },
  {
    path: '/KafkaStarter/answers',
    component: ComponentCreator('/KafkaStarter/answers', '59d'),
    exact: true
  },
  {
    path: '/KafkaStarter/curriculum',
    component: ComponentCreator('/KafkaStarter/curriculum', '91c'),
    routes: [
      {
        path: '/KafkaStarter/curriculum',
        component: ComponentCreator('/KafkaStarter/curriculum', '24f'),
        routes: [
          {
            path: '/KafkaStarter/curriculum',
            component: ComponentCreator('/KafkaStarter/curriculum', 'a44'),
            routes: [
              {
                path: '/KafkaStarter/curriculum/CURRICULUM_GUIDE',
                component: ComponentCreator('/KafkaStarter/curriculum/CURRICULUM_GUIDE', '076'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_1/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_1/concept', '987'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_10/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_10/concept', 'c6e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_11/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_11/concept', 'bfd'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_12/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_12/concept', '7e6'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_13/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_13/concept', '3ed'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_14/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_14/concept', 'a43'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_15/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_15/concept', '27f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_16/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_16/concept', '44e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_17/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_17/concept', 'a0e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_18/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_18/concept', 'b5b'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_19/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_19/concept', '4e0'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_2/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_2/concept', 'b03'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_20/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_20/concept', '33a'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_3/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_3/concept', 'af5'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_3/workshop_3',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_3/workshop_3', '985'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_4/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_4/concept', '25e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_4/workshop_4',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_4/workshop_4', 'f5f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_5/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_5/concept', 'b32'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_5/workshop_5',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_5/workshop_5', '5a0'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_6/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_6/concept', '1d3'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_6/workshop_6',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_6/workshop_6', '3a5'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_7/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_7/concept', '1be'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_8/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_8/concept', '108'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_9/concept',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_9/concept', 'e09'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/lesson_9/workshop_9',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/lesson_9/workshop_9', '607'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/modules/practical_usecases_guideline',
                component: ComponentCreator('/KafkaStarter/curriculum/modules/practical_usecases_guideline', 'e81'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_1/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_1/', '1f0'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_10/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_10/', '47e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_11/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_11/', '538'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_12/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_12/', '0be'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_13/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_13/', '7bf'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_14/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_14/', '8cf'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_15/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_15/', 'ce4'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_16/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_16/', '3b5'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_17/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_17/', 'f46'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_18/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_18/', 'f6f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_19/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_19/', 'ce7'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_2/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_2/', '9f0'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_20/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_20/', '433'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_3/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_3/', 'dd9'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_4/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_4/', 'ace'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_5/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_5/', 'f97'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_6/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_6/', '959'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_7/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_7/', '640'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_8/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_8/', 'c66'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/curriculum/workshop/lesson_9/',
                component: ComponentCreator('/KafkaStarter/curriculum/workshop/lesson_9/', 'c68'),
                exact: true,
                sidebar: "tutorialSidebar"
              }
            ]
          }
        ]
      }
    ]
  },
  {
    path: '/KafkaStarter/',
    component: ComponentCreator('/KafkaStarter/', '855'),
    exact: true
  },
  {
    path: '*',
    component: ComponentCreator('*'),
  },
];
