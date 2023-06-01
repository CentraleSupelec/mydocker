---
hide:
  - toc
---

MyDocker fournit à vos étudiants des environnements numériques:

- accessibles directement depuis Moodle (ou autre LMS compatible LTI),
- hébergés sur vos serveurs ou dans le cloud (techn ologie Docker),
- aussi bien pour des séances de TPs que pour des projets en autonomie.

En bref, avec MyDocker :

- vous n'êtes pas dépendant de la disponibilité de salles informatiques ni de leur configuration logicielle,
- vous ne perdez pas de temps à corriger les installations logicielles sur les postes des étudiants (en général un simple navigateur suffit),
- vous garantissez l'équité entre étudiants en fournissant à chacun les mêmes ressources informatiques,
- vous protégez les traces d'apprentissage de vos étudiants.

<figure markdown>
  ![Image title](assets/student_xp_vid.gif){ width="750" }
  <figcaption>Interface étudiant de connexion à un TP sur MyDocker</figcaption>
</figure>

MyDocker est utilisé en production à [CentraleSupélec](http://www.centralesupelec.fr) depuis 2019 pour des enseignements d'intelligence artificielle, de développement informatique, de simulation numérique, de modélisation,...

Son architecture permet de passer à l'échelle en combinant l'usage de ressources informatiques locales ou dans le Cloud que ce soit sur des environnements CPU ou GPU.

De plus, dans MyDocker les environnements sont indépendants les uns des autres: fini les conflits de versions de bibliothèques, l'enseignant est libre de choisir quelles versions de logiciel il veut utiliser.

<figure markdown>
  ![Image title](assets/video-library.gif){ width="750" }
  <figcaption>De multiples environnements Docker réutilisables</figcaption>
</figure>

Il est aussi possible de collaborer à plusieurs enseignants sur des images MyDocker, ce qui permet de constituer une bibliothèque d'environnements réutilisables. D'ailleurs, la plupart du temps, il suffit d'adapter un environnement existant pour créer un nouveau TP ou projet.

Enfin MyDocker est respectueux des données des étudiants: votre établissement conserve la maitrise des traces d'apprentissage.

[Accès au code source](https://github.com/CentraleSupelec/mydocker){ .md-button }
[Expérience Enseignant](/mydocker/user_guide/){ .md-button }
[Expérience Etudiant](/mydocker/student_xp/){ .md-button }
[Contactez nous](mailto:contact.opensource@centralesupelec.fr){ .md-button }

