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

import { Component, ViewChild } from "@angular/core";
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors } from "@angular/forms";
import { BreadcrumbsService } from "../breadcrumbs/breadcrumbs.service";
import { MrDatasetNature } from "../datasets/dataset/mr/dataset.mr.model";
import { slideDown } from "../shared/animations/animations";
import { Pageable } from "../shared/components/table/pageable.model";
import { TableComponent } from "../shared/components/table/table.component";
import { DatepickerComponent } from "../shared/date-picker/date-picker.component";
import { DatasetModalityType } from "../shared/enums/dataset-modality-type";
import { IdName } from "../shared/models/id-name.model";
import { Subject } from "../subjects/shared/subject.model";
import { FacetField, ShanoirSolrFacet, SolrResultPage } from "./solr.document.model";
import { SolrService } from "./solr.service";

@Component({
    selector: 'solr-search',
    templateUrl: 'solr.search.component.html',
    styleUrls: ['solr.search.component.css'],
    animations: [slideDown]
})

export class SolrSearchComponent{
    studies: IdName[] = [];
    selectedStudies: IdName[] = [];
    facets: any[] = [];
    // filteredSubjectNames: Observable<string[]>;
    allMrDatasetNatures: any[];
    allDatasetModalityTypes: any[];
    solrRequest: ShanoirSolrFacet = new ShanoirSolrFacet();
    columnDefs: any[];
    form: FormGroup;
    @ViewChild('table') table: TableComponent;

    constructor(
            private breadcrumbsService: BreadcrumbsService, private formBuilder: FormBuilder,
            private solrService: SolrService) {
        
        this.form = this.buildForm();
        this.breadcrumbsService.markMilestone();
        this.breadcrumbsService.nameStep('Solr Search'); 
        this.columnDefs = this.getColumnDefs();
        this.allMrDatasetNatures = MrDatasetNature.getValueLabelJsonArray();
        this.allDatasetModalityTypes = DatasetModalityType.getValueLabelJsonArray(); 
    }
    
    // ngOnInit() {
    //     this.filteredSubjectNames = this.form.get('subject').valueChanges
    //     .pipe(
    //       startWith(''),
    //       map(value => this.filterSubjectName(value))
    //     );
    // }

    buildForm(): FormGroup {
        let formGroup = this.formBuilder.group({
            'studyName': [this.solrRequest.studyName],
            'subjectName': [this.solrRequest.subjectName],
            'examinationComment': [{value: this.solrRequest.examinationComment, disabled: !this.solrRequest.subjectName || this.selectedStudies.length < 1}],
            'datasetName': [{value: this.solrRequest.datasetName, disabled: !this.solrRequest.subjectName || this.selectedStudies.length < 1}],
            'startDate': [this.solrRequest.datasetStartDate, [DatepickerComponent.validator]],
            'endDate': [this.solrRequest.datasetEndDate, [DatepickerComponent.validator, this.dateOrderValidator]],
            'datasetTypes': [this.solrRequest.datasetTypes],
            'datasetNatures': [this.solrRequest.datasetNatures]
        });
        return formGroup;
    }

    formErrors(field: string): any {
        if (!this.form) return;
        const control = this.form.get(field);
        if (control && control.touched && !control.valid) {
            return control.errors;
        }
    }

    hasError(fieldName: string, errors: string[]) {
        let formError = this.formErrors(fieldName);
        if (formError) {
            for(let errorName of errors) {
                if(formError[errorName]) return true;
            }
        }
        return false;
    }

    dateOrderValidator = (control: AbstractControl): ValidationErrors | null => {
        if (this.solrRequest.datasetStartDate && this.solrRequest.datasetEndDate && this.solrRequest.datasetStartDate > this.solrRequest.datasetEndDate) {
            return { order: true }
        }
        return null;
    }
    
    getPage(pageable: Pageable): Promise<SolrResultPage> {
        let saveStates = [];
        if (this.solrRequest.studyName) saveStates[0] = this.solrRequest.studyName.slice();
        if (this.solrRequest.subjectName) saveStates[1] = this.solrRequest.subjectName.slice();
        if (this.solrRequest.examinationComment) saveStates[2] = this.solrRequest.examinationComment.slice();
        if (this.solrRequest.datasetName) saveStates[3] = this.solrRequest.datasetName.slice();
        return this.solrService.facetSearch(this.solrRequest, pageable).then(solrResultPage => {
            for (let j = 0; j < solrResultPage['facetResultPages'].length; j++) {
                let facet: any[] = [];
                for (let i = 0; i < solrResultPage['facetResultPages'][j].content.length; i++) {
                    let facetField: FacetField = new FacetField(solrResultPage['facetResultPages'][j].content[i]);
                    if (saveStates[j] && facetField.value == saveStates[j]) {facetField.checked = true;}
                    facet.push(facetField);
                }
                this.facets[j] = facet;
            }
            return solrResultPage;
        });
    }

    // Grid columns definition
    getColumnDefs() {
        function dateRenderer(date: number) {
            if (date) {
                return new Date(date).toLocaleDateString();
            }
            return null;
        };
        return [
            {headerName: "Id", field: "datasetId", type: "number", width: "30px", defaultSortCol: true, defaultAsc: false},
            {headerName: "Name", field: "datasetName"},
            {headerName: "Type", field: "datasetType", width: "30px"},
            {headerName: "Nature", field: "datasetNature", width: "30px"},
            {headerName: "Creation", field: "datasetCreationDate", type: "date", cellRenderer: (params: any) => dateRenderer(params.data.datasetCreationDate)},
            {headerName: "Study", field: "studyName"},
            {headerName: "Subject", field: "subjectName"},
            {headerName: "Exam", field: "examinationComment"},
            {headerName: "Exam Date", field:"examinationDate", type: "date", cellRenderer: (params: any) => dateRenderer(params.data.examinationDate)}
        ];
    }

    private removeAllFacets() {
        this.solrRequest.studyName = this.solrRequest.subjectName = this.solrRequest.examinationComment = this.solrRequest.datasetName = null;
    }

    private showAllStudies() {
        this.solrRequest.studyName = null;
    }
    private isStudyAlreadySelected (studyId: number) {
        if (this.selectedStudies.filter(study => study.id == studyId).length > 0) return true;
        else return false;
    }

    private removeFromSelectedStudies(studyId: number) {
        this.selectedStudies = this.selectedStudies.filter(study => study.id !== studyId);
    }


    private removeSelectedSubject(subject: Subject) {
        console.log(subject)
    }

    // private filterSubjectName(value: string): string[] {
    //     const filterValue = value.toLowerCase();
    //     return this.subjectNames.filter(option => option.toLowerCase().includes(filterValue));
    // }
}