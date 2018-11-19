import {Component, Input, Output, EventEmitter, ViewChild} from '@angular/core'

import { AnestheticIngredient } from '../shared/anestheticIngredient.model';
import { AnestheticIngredientService } from '../shared/anestheticIngredient.service';
import { Anesthetic } from '../../anesthetic/shared/anesthetic.model';
import { AnestheticType } from "../../../shared/enum/anestheticType";
import { ModesAware } from "../../../shared/mode/mode.decorator";
import { TableComponent } from '../../../../shared/components/table/table.component';
import { BrowserPaginEntityListComponent } from '../../../../shared/components/entity/entity-list.browser.component.abstract';
import { ShanoirError } from '../../../../shared/models/error.model';
import { ServiceLocator } from '../../../../utils/locator.service';

export type Mode =  "view" | "edit" | "create";


@Component({
  selector: 'ingredients-list',
  templateUrl:'anestheticIngredient-list.component.html',
  styleUrls: ['anestheticIngredient-list.component.css'], 
  providers: [AnestheticIngredientService]
})

@ModesAware
export class AnestheticIngredientsListComponent  extends BrowserPaginEntityListComponent<AnestheticIngredient> {
    
    @Input() mode:Mode ;
    @Input() canModify: Boolean = false;
    @Input() anesthetic: Anesthetic;
    public toggleFormAI: boolean = false;
    public createAIMode: boolean = false;
    public ingredientSelected : AnestheticIngredient;
    @Output() onIngredientAdded = new EventEmitter();
    @Output() onIngredientDeleted = new EventEmitter();
    @ViewChild('ingredientsTable') table: TableComponent;

    

    constructor(
        private ingredientsService: AnestheticIngredientService) {
            super('preclinical-anesthetic-ingredient');
     }
    
    getEntities(): Promise<AnestheticIngredient[]> {
        if (this.anesthetic && this.anesthetic.id){
            return this.ingredientsService.getIngredients(this.anesthetic);
        }else{
            return Promise.resolve([]);
        }
    }
    
    getColumnDefs(): any[] {
        function checkNullValue(value: any) {
            if(value){
                return value;
            }
            return '';
        };
        function checkNullValueReference(reference: any) {
            if(reference){
                return reference.value;
            }
            return '';
        };
        let colDef: any[] = [
            {headerName: "Name", field: "name.value", type: "reference", cellRenderer: function (params: any) {
                return checkNullValueReference(params.data.name);
            }},
            {headerName: "Concentration", field: "concentration", type: "number", cellRenderer: function (params: any) {
                return checkNullValue(params.data.concentration);
            }},
            {headerName: "Concentration Unit", field: "concentration_unit.value", type: "reference", cellRenderer: function (params: any) {
                return checkNullValueReference(params.data.concentration_unit);
            }}    
        ];
        return colDef;       
    }

    getCustomActionsDefs(): any[] {
        return [];
    }
    
    
    generateAnestheticName(){
        if(this.anesthetic){
            let generatedName = '';
            if(this.anesthetic.anestheticType) generatedName = generatedName.concat(AnestheticType[this.anesthetic.anestheticType]).concat(' ');
            if(this.anesthetic.ingredients){
                for(let ingredient of this.anesthetic.ingredients){
                    let strIngredient = '';
                    strIngredient = strIngredient.concat(ingredient.name.value.substring(0,3)).concat('. ');
                    if(ingredient.concentration) strIngredient = strIngredient.concat(String(ingredient.concentration));
                    if(ingredient.concentration_unit) strIngredient = strIngredient.concat(ingredient.concentration_unit.value);
                    strIngredient = strIngredient.concat(' ');
                    if(generatedName.indexOf(strIngredient) < 0){
                        generatedName = generatedName.concat(strIngredient);
                    }            
                }
            }
            this.onIngredientAdded.emit(this.anesthetic.ingredients);
            
        }
    }
        
    refreshDisplay(cancel:boolean){
        this.toggleFormAI = false;
        this.createAIMode = false;
        this.generateAnestheticName();
        this.table.refresh();
    }
    
    protected openDeleteConfirmDialog = (entity: AnestheticIngredient) => {
        if (this.keycloakService.isUserGuest()) return;
        this.getSelectedIngredient(entity.id).then(selectedIngredient => {
            this.confirmDialogService
                .confirm(
                    'Delete', 'Are you sure you want to delete preclinical-anesthetic-ingredient n° ' + entity.id + ' ?',
                    ServiceLocator.rootViewContainerRef
                ).subscribe(res => {
                    if (res) {
                        this.ingredientsService.deleteAnestheticIngredient(this.anesthetic.id, entity.id).then((response) => {
                            this.getAnestheticIngredient(selectedIngredient)
                            this.onDelete.next(selectedIngredient);
                            let index = this.anesthetic.ingredients.findIndex(i => i.id === entity.id); //find index in your array
                            this.anesthetic.ingredients.splice(index, 1);
                            this.table.refresh();
                            this.msgBoxService.log('info', 'The preclinical-anesthetic-ingredient sucessfully deleted');
                        }).catch(reason => {
                            if (reason && reason.error) {
                                this.onDelete.next(new ShanoirError(reason));
                                if (reason.error.code != 422) throw Error(reason);
                            }
                        });                    
                    }
                });
        });
    }

    private getSelectedIngredient(id : number): Promise<AnestheticIngredient>{
        return this.ingredientsService.get(id).then(anestheticIngredient => {
            return anestheticIngredient;
        }
        );
    }

    getAnestheticIngredient(ingredient: AnestheticIngredient): void {
    	this.onIngredientDeleted.emit(ingredient);
      	this.generateAnestheticName();
    }
    
    
    viewIngredient = (ingredient: AnestheticIngredient) => {
        this.toggleFormAI = true;
        this.createAIMode = false;
        this.ingredientSelected = ingredient;
        //this.generateAnestheticName();
    }
    
    toggleIngredientForm(){
    	this.ingredientSelected = new AnestheticIngredient();
        this.createAIMode = true;
        if(this.toggleFormAI==false){
            this.toggleFormAI = true;
        }else if(this.toggleFormAI==true){
            this.toggleFormAI = false;
        }else{
            this.toggleFormAI = true;
        }
    }
    
    
}