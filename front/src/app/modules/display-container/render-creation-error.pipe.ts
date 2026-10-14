import { Pipe, PipeTransform } from "@angular/core";
import { IContainer } from "../shell/interfaces/container";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";

@Pipe({
  pure: false,
  name: "renderCreationError",
})
export class RenderCreationErrorPipe implements PipeTransform {
  constructor(
    private readonly router: Router,
    private readonly translate: TranslateService
  ) {
  }

  transform(container: IContainer | null) {
    if (!container) {
      return this.translate.instant('container.unknown_error');
    }
    switch (container.creationError) {
      case "student-volume.local-storage":
        return this.translate.instant('container.storage_error');
      case "student-volume.existing-rbd-service":
        const date = container.errorParams["createdAt"] ? new Date(container.errorParams["createdAt"]) : null;
        const now = new Date();
        const courseTitle = container.errorParams["courseTitle"] ?? '';
        const timeString = date ? `${this.translate.instant('container.at')} ${date?.toLocaleTimeString(undefined, {
          hour: "2-digit",
          minute: "2-digit",
        }).replace(/:/, "h")}` : this.translate.instant('container.recently');
        const dateString = date ? `${this.translate.instant('container.on')} ${date?.toLocaleDateString()}` : '';
        const dateParts = [timeString];
        if (date && (date?.getDay() !== now?.getDay() || date?.getMonth() !== now.getMonth())) {
          dateParts.unshift(dateString);
        }
        const formattedDate = dateParts.join(' ');
        return this.translate.instant('container.personal_volume_error', {
          url: this.router.createUrlTree(
              ["/shell"],
              {queryParams: {course_id: container.errorParams["courseId"]}},
            ).toString(),
          course: courseTitle,
          date: formattedDate
        })
      default:
        if (container.creationError?.startsWith("No such image:")) {
          return this.translate.instant('container.image_not_found_error');
        } else {
          return this.translate.instant('container.problem_encountered', { error: container.creationError});
        }
    }
  }
}
