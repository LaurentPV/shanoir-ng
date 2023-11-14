/**
 * Shanoir NG - Import, manage and share neuroimaging data
 * Copyright (C) 2009-2019 Inria - https://www.inria.fr/
 * Contact us on https://project.inria.fr/shanoir/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html
 */

import { HttpClient, HttpParams } from '@angular/common/http';
import {Component, ComponentRef, ElementRef, EventEmitter, HostListener, Input, Output, ViewChild} from '@angular/core';
import {Study} from "../../../studies/shared/study.model";
import {UntypedFormBuilder, UntypedFormGroup} from "@angular/forms";
import { GlobalService } from '../../services/global.service';
import {StudyStorageVolumeDTO} from "../../../studies/shared/study.dto";
import * as AppUtils from "../../../utils/app.utils";
import {KeycloakService} from "../../keycloak/keycloak.service";
import {StudyUserRight} from "../../../studies/shared/study-user-right.enum";
import {StudyRightsService} from "../../../studies/shared/study-rights.service";
import {ServiceLocator} from "../../../utils/locator.service";
import {ConsoleService} from "../../console/console.service";
import {DatasetService} from "../../../datasets/shared/dataset.service";
import {StudyService} from "../../../studies/shared/study.service";
import {SolrDocument} from "../../../solr/solr.document.model";
import {ShanoirError} from "../../models/error.model";

@Component({
    selector: 'user-action-dialog',
    templateUrl: 'dataset-copy-dialog.component.html',
    styleUrls: ['dataset-copy-dialog.component.css']
})
export class DatasetCopyDialogComponent {
    title: string;
    message: string;
    studies: Study[];
    selectedStudy: Study;
    datasetsIds: number[];
    statusMessage: string;
    ownRef: any;
    hasRight: boolean = false;
    isDatasetInStudy: boolean = false;
    canCopy: boolean;
    centerIds: string[]=[];
    subjectIds: string[]=[];
    lines: SolrDocument[];
    datasetSubjectIds: string[]=[];
    datasetExamIds: string[]=[];
    protected consoleService = ServiceLocator.injector.get(ConsoleService);
    constructor(private http: HttpClient,
                private studyRightsService: StudyRightsService,
                private studyService: StudyService,
                private keycloakService: KeycloakService) {
    }

    ngOnInit() {
        for (let line of this.lines) {
            if (!this.centerIds.includes(line.centerId)) {
                this.centerIds.push(line.centerId);
            }
            if (!this.subjectIds.includes(line.subjectId)) {
                this.subjectIds.push(line.subjectId);
            }
            if (!this.datasetSubjectIds.includes(line.datasetId + "/" + line.subjectId)) {
                this.datasetSubjectIds.push(line.datasetId + "/" + line.subjectId);
            }
        }
    }
    public copy() {
        this.checkRightsOnSelectedStudies(this.selectedStudy.id).then( () => {
            this.isDatasetInStudy = this.checkDatasetBelongToStudy(this.lines, this.selectedStudy.id);

            if (!this.hasRight) {
                this.statusMessage = 'Missing rights for study ' + this.selectedStudy.name + ' please make sure you have ADMIN right.';
            } else if (this.isDatasetInStudy) {
                this.statusMessage = 'Selected dataset(s) already belong to selected study.';
            } else {
                this.statusMessage = "Start copy...";
                const formData: FormData = new FormData();
                formData.set('datasetIds', Array.from(this.datasetsIds).join(","));
                formData.set('studyId', this.selectedStudy.id.toString());
                formData.set('centerIds', Array.from(this.centerIds).join(","));
                formData.set('datasetSubjectIds', Array.from(this.datasetSubjectIds).join(","));
                formData.set('subjectIds', Array.from(this.subjectIds).join(","));
                return this.http.post<string>(AppUtils.BACKEND_API_STUDY_URL + '/copyDatasets', formData, { responseType: 'text' as 'json'})
                    .toPromise()
                    .then(res => {
                        console.log("res : " + res);
                        this.statusMessage = res;
                    }).catch(reason => {
                        if (reason.status == 403) {
                            this.statusMessage = "You must be admin or expert.";
                        } else throw Error(reason);
                    });
            }
        });
    }

    public checkDatasetBelongToStudy(lines: SolrDocument[], studyId: number) {
        return lines.some((line) => {
            return (studyId == Number(line.studyId));
        });
    }

    private async checkRightsOnSelectedStudies(id: number): Promise<void> {
        await this.hasRightsOnStudyId(id).then(res => {
            this.hasRight = res;
        });
    }

    private async hasRightsOnStudyId(studyId: number): Promise<boolean> {
        if (this.keycloakService.isUserAdmin()) {
            return Promise.resolve(true);
        } else {
            return this.studyRightsService.getMyRightsForStudy(studyId).then(rights => {
                return (rights.includes(StudyUserRight.CAN_ADMINISTRATE));
            });
        }
    }

    pickStudy(study: Study) {
        this.selectedStudy = study;
    }

    cancel() {
        this.ownRef.destroy();
    }
}
