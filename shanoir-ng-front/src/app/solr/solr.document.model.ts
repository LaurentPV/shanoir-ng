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

import { Page } from "../shared/components/table/pageable.model";

export class SolrDocument {
    datasetId: string;
    datasetName: string;
    datasetType: string;
    datasetNature: string;
    datasetCreationDate: Date;
    examinationComment: string;
    examinationDate: Date;
    subjectName: string;
    studyName: string;
    studyId: string;
}

export class ShanoirSolrFacet {
    studyName: string[];
    subjectName: string[];
    examinationComment: string[];
    datasetName: string[];
    datasetStartDate: Date;
    datasetEndDate: Date;
    datasetTypes: string[];
    datasetNatures: string[];
}

export class FacetField {
    field: string;
    value: string;
    valueCount: number;
    checked: boolean;

    constructor (facetField: FacetField) {
        this.field = facetField.field;
        this.value = facetField.value;
        this.valueCount = facetField.valueCount;
    }
}

export class FacetResultPage {
    public content: FacetField[];
}

export class SolrResultPage extends Page<SolrDocument>{
    public facetResultPages: FacetResultPage[];
}