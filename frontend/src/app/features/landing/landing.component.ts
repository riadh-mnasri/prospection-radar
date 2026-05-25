import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent {
  readonly year = new Date().getFullYear();
  readonly features = [
    {
      icon: 'radar',
      title: 'Radar multi-sources',
      desc: 'Scrapez automatiquement Free-Work, Hellowork, LinkedIn et plus — toutes les missions Java Tech Lead en un seul endroit.',
      color: 'purple'
    },
    {
      icon: 'ai',
      title: 'Scoring IA par Claude',
      desc: 'Chaque mission est analysée par Claude AI et scorée de 0 à 100 selon votre profil exact : compétences, TJM, remote.',
      color: 'emerald'
    },
    {
      icon: 'signal',
      title: 'Marché caché',
      desc: 'Levées de fonds, nominations CTO, recrutements massifs — détectez les opportunités avant qu\'elles soient publiées.',
      color: 'amber'
    },
    {
      icon: 'message',
      title: 'Messages personnalisés',
      desc: 'Claude rédige pour vous des messages LinkedIn et emails de contact adaptés à chaque mission et décideur identifié.',
      color: 'blue'
    }
  ];

  readonly steps = [
    { num: '01', title: 'Configurez votre profil', desc: 'Compétences, TJM cible, préférence remote — le radar s\'adapte à vous.' },
    { num: '02', title: 'Lancez un scan', desc: 'En un clic, le radar parcourt toutes les sources et détecte les opportunités.' },
    { num: '03', title: 'Contactez en priorité', desc: 'Triez par score, ajoutez en favoris, et envoyez le message généré par IA.' }
  ];
}
