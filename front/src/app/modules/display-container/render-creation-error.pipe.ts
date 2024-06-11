import { Pipe, PipeTransform } from "@angular/core";
import { IContainer } from "../shell/interfaces/container";
import { Router } from "@angular/router";

@Pipe({
  pure: false,
  name: "renderCreationError",
})
export class RenderCreationErrorPipe implements PipeTransform {
  constructor(private readonly router: Router) {
  }

  transform(container: IContainer | null) {
    if (!container) {
      return "une erreur inconnue s'est produite.";
    }
    switch (container.creationError) {
      case "student-volume.local-storage":
        return "le cours est configuré avec un type de fichiers non supporté (local).";
      case "student-volume.existing-rbd-service":
        const date = container.errorParams["createdAt"] ? new Date(container.errorParams["createdAt"]) : null;
        const now = new Date();
        const courseTitle = container.errorParams["courseTitle"] ?? '';
        const timeString = date ? `à ${date?.toLocaleTimeString(undefined, {
          hour: "2-digit",
          minute: "2-digit",
        }).replace(/:/, "h")}` : "récemment";
        const dateString = date ? `le ${date?.toLocaleDateString()}` : '';
        const dateParts = [timeString];
        if (date && (date?.getDay() !== now?.getDay() || date?.getMonth() !== now.getMonth())) {
          dateParts.unshift(dateString);
        }
        const formattedDate = dateParts.join(' ');
        return `le service <a href="${this.router.createUrlTree(
          ["/shell"],
          {queryParams: {course_id: container.errorParams["courseId"]}},
        ).toString()}">${courseTitle}</a> démarré ${formattedDate} utilise actuellement le répertoire personnel. Il est nécessaire de l'éteindre au préalable.`;
    }
    return container.creationError;
  }
}
