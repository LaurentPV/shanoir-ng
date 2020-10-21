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
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Component, ElementRef, Input, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';

import { TreeNodeComponent } from '../../shared/components/tree/tree-node.component';
import { GlobalService } from '../../shared/services/global.service';
import * as AppUtils from '../../utils/app.utils';
import { ServiceLocator } from '../../utils/locator.service';
import { BidsElement } from '../model/bidsElement.model';



@Component({
    selector: 'bids-tree',
    templateUrl: 'bids-tree.component.html',
    styleUrls: ['bids-tree.component.css'],
})

export class BidsTreeComponent implements OnDestroy {

    API_URL = AppUtils.BACKEND_API_BIDS_URL;
    protected http: HttpClient = ServiceLocator.injector.get(HttpClient);

    @Input() list: BidsElement[];
    @Input() studyId: number;
    protected json: JSON;
    protected tsv: string[][];
    protected title: string;
    protected selectedIndex: string;
    private globalClickSubscription: Subscription;

    constructor(private globalService: GlobalService, private elementRef: ElementRef) {
        this.globalClickSubscription = globalService.onGlobalClick.subscribe(clickEvent => {
            if (!this.elementRef.nativeElement.contains(clickEvent.target)) {
                this.selectedIndex = null;
                this.removeContent();
            }
        }) 
    }

    ngOnDestroy(): void {
        this.globalClickSubscription.unsubscribe();
    }

    getFileName(element): string {
        return element.split('\\').pop().split('/').pop();
    }

    getDetail(component: TreeNodeComponent) {
        component.dataLoading = true;
        component.hasChildren = true;
        component.open();
    }

    getContent(bidsElem: BidsElement, id: string) {
        this.removeContent();
        if (id == this.selectedIndex) {
            this.selectedIndex = null;
            return;
        }
        this.selectedIndex = id;
        if (bidsElem.content) {
            this.title = this.getFileName(bidsElem.path);
            if (bidsElem.path.indexOf('.json') != -1) {
                this.json = JSON.parse(bidsElem.content);
            } else if (bidsElem.path.indexOf('.tsv') != -1) {
                this.tsv = this.parseTsv(bidsElem.content);
            }
        }
    }

    private parseTsv(tsv: string): string[][] {
        return tsv.split('\n').map(line => line.split('\t'));
    }

    removeContent() {
        this.title = null;
        this.tsv = null;
        this.json = null;
    }

    protected download(item: BidsElement): void {
        const endpoint = this.API_URL + "/exportBIDS/studyId/" + this.studyId;
        let params = new HttpParams().set("filePath", item.path);
        
        this.http.get(endpoint, { observe: 'response', responseType: 'blob', params: params }).subscribe(response => {
            if (response.status == 200) {
                this.downloadIntoBrowser(response);
            }
        });
    }

    private getFilename(response: HttpResponse<any>): string {
        const prefix = 'attachment;filename=';
        let contentDispHeader: string = response.headers.get('Content-Disposition');
        return contentDispHeader.slice(contentDispHeader.indexOf(prefix) + prefix.length, contentDispHeader.length);
    }

    private downloadIntoBrowser(response: HttpResponse<Blob>){
        AppUtils.browserDownloadFile(response.body, this.getFilename(response));
    }


}