import { IAdminCourse } from "../interfaces/course";

const DEFAULT_COMMAND = "{{USERNAME}} {{PASSWORD}}";

export class DefaultCourseFormValuesService {
  static getTechnicalValues(obj: IAdminCourse, defaultComputeTypeId?: number, shrinkLimits = false): {
    [key: string]: any;
  } {
    const multiplier = shrinkLimits ? 1e-9 : 1;
    return {
      ports: obj?.ports ?? [],

      dockerImage: obj?.dockerImage ?? "",
      nanoCpusLimit: obj?.nanoCpusLimit * multiplier  ?? null,
      memoryBytesLimit: obj?.memoryBytesLimit * multiplier  ?? null,
      computeTypeId: obj?.computeTypeId ?? defaultComputeTypeId,
      command: obj?.command ?? DEFAULT_COMMAND,

      saveStudentWork: obj?.saveStudentWork ?? false,
      workdirSize: obj?.workdirSize ?? null,
      workdirPath: obj?.workdirPath ?? null,
      allowStudentToSubmit: obj?.allowStudentToSubmit ?? false,

      displayOptions: obj?.displayOptions ?? {},
      useStudentVolume: obj?.useStudentVolume ?? false,
      studentVolumePath: obj?.studentVolumePath ?? null,
    };
  }
}
