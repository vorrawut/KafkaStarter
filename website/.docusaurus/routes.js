import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/KafkaStarter/answers',
    component: ComponentCreator('/KafkaStarter/answers', '59d'),
    exact: true
  },
  {
    path: '/KafkaStarter/',
    component: ComponentCreator('/KafkaStarter/', '855'),
    exact: true
  },
  {
    path: '/KafkaStarter/',
    component: ComponentCreator('/KafkaStarter/', '1c1'),
    routes: [
      {
        path: '/KafkaStarter/',
        component: ComponentCreator('/KafkaStarter/', '1d7'),
        routes: [
          {
            path: '/KafkaStarter/',
            component: ComponentCreator('/KafkaStarter/', '6d9'),
            routes: [
              {
                path: '/KafkaStarter/CURRICULUM_GUIDE',
                component: ComponentCreator('/KafkaStarter/CURRICULUM_GUIDE', '0be'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/LESSON_TEMPLATE',
                component: ComponentCreator('/KafkaStarter/LESSON_TEMPLATE', 'fff'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_1/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_1/concept', 'a38'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_10/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_10/concept', 'b31'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_11/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_11/concept', '5f5'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_12/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_12/concept', '581'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_13/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_13/concept', '997'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_14/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_14/concept', '4b1'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_15/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_15/concept', '96a'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_16/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_16/concept', '697'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_17/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_17/concept', '115'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_18/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_18/concept', '784'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_19/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_19/concept', 'd8d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_2/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_2/concept', 'd46'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_20/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_20/concept', 'fb7'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_3/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_3/concept', 'a3d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_3/workshop_3',
                component: ComponentCreator('/KafkaStarter/modules/lesson_3/workshop_3', 'f44'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_4/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_4/concept', 'aad'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_4/workshop_4',
                component: ComponentCreator('/KafkaStarter/modules/lesson_4/workshop_4', '652'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_5/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_5/concept', '6b7'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_5/workshop_5',
                component: ComponentCreator('/KafkaStarter/modules/lesson_5/workshop_5', '5a6'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_6/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_6/concept', 'daf'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_6/workshop_6',
                component: ComponentCreator('/KafkaStarter/modules/lesson_6/workshop_6', '1b4'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_7/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_7/concept', '4dc'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_8/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_8/concept', '7ba'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_9/concept',
                component: ComponentCreator('/KafkaStarter/modules/lesson_9/concept', '36d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/modules/lesson_9/workshop_9',
                component: ComponentCreator('/KafkaStarter/modules/lesson_9/workshop_9', '38d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_1/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_1/', 'f9d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_10/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_10/', '1bc'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_11/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_11/', 'a5f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_12/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_12/', '6d0'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_13/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_13/', 'b16'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_14/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_14/', '708'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_15/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_15/', 'e6a'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_16/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_16/', 'e96'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_17/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_17/', '143'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_18/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_18/', '99f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_19/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_19/', '69c'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_2/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_2/', 'd45'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_20/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_20/', '42f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_3/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_3/', 'e87'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_4/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_4/', '374'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_5/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_5/', '739'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_6/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_6/', '90d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_7/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_7/', 'ce3'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_8/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_8/', 'e8c'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/KafkaStarter/workshop/lesson_9/',
                component: ComponentCreator('/KafkaStarter/workshop/lesson_9/', '24c'),
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
    path: '*',
    component: ComponentCreator('*'),
  },
];
