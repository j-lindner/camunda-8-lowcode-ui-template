import store, { AppThunk } from '../store';
import { loadStart, loadSuccess, setCurrentForm, setFormName, setCurrentFormPreview, fail, silentfail } from '../store/features/adminForms/slice';
import { FormBuilder } from 'formiojs';
import { newFormEditor } from '@camunda-community/form-js-extended-jens';
import api from './api';

var currentFormEditor: any;
var currentFormBuilder: any;

export class AdminFormService {
  lastFetch: number = 0;
  lastFormEditor: any = null;
  formEditorCreating = false;
  getDefaultForm = (formType: string): any => {
    if (formType == 'formJs' || formType == 'extendedFormJs') {
      return {
        name: 'New Form',
        generator: formType,
        schema: {
          components: [],
          schemaVersion: 4,
          type: "default",
          id: "Form_" + Math.floor(1000000 + Math.random() * 9000000),
          executionPlatform: "Camunda Cloud",
          executionPlatformVersion: "1.1",
          exporter: {
            name: "Camunda Modeler",
            version: "5.0.0"
          }
        },
        previewData: '{}'
      }
    } else {
      return {
        name: 'New Form',
        generator: formType,
        schema: {
          "_id": Math.floor(1000000 + Math.random() * 9000000),
          "components": []
        },
        previewData: '{}'
      }
    }
  }
  geForms = (): AppThunk => async dispatch => {
    if (this.lastFetch < Date.now() - 5000) {
      try {
        dispatch(loadStart());
        const { data } = await api.get<string[]>('/edition/forms/names');
        dispatch(loadSuccess(data));
      } catch(error: any) {
        if (error.response) {
          // The request was made. server responded out of range of 2xx
          dispatch(fail(error.response.data.message));
        } else if (error.request) {
          // The request was made but no response was received
          dispatch(fail('ERROR_NETWORK'));
        } else {
          // Something happened in setting up the request that triggered an Error
          console.warn('Error', error.message);
          dispatch(fail(error.toString()));
        }
      }
      this.lastFetch = Date.now();
    }
  }
  newForm = (formType: string): AppThunk => async dispatch => {
    dispatch(setCurrentForm(this.getDefaultForm(formType)));
  }
  openForm = (name:string): AppThunk => async dispatch => {
    api.get('/edition/forms/' + name).then(response => {
      let form = response.data;
      form.previewData = JSON.stringify(form.previewData, null, 2);

      dispatch(setCurrentForm(form));
    }).catch(error => {
      alert(error.message);
    })
  }
  deleteForm = (name: string): AppThunk => async dispatch => {
    api.delete('/edition/forms/' + name).then(response => {
      dispatch(this.geForms());
    }).catch(error => {
      alert(error.message);
    })
  }
  setForm = (form: any): AppThunk => async dispatch => {
    dispatch(setCurrentForm(form));
  }
  setFormName = (formName: string): AppThunk => async dispatch => {
    dispatch(setFormName(formName));
  }
  setFormEditor = (formEditor: any): AppThunk => async dispatch => {
    currentFormEditor = formEditor;
    currentFormBuilder = null;
  }
  getFormEditor = () => {
    return currentFormEditor;
  }
  setFormBuilder = (formBuilder: FormBuilder): AppThunk => async dispatch => {
    currentFormBuilder = formBuilder;
    currentFormEditor = null;
  }
  getFormBuilder = () => {
    return currentFormBuilder;
  }
  getSchema = () => {
    if (currentFormEditor/*store.getState().adminForms.formEditor*/) {
     return currentFormEditor.saveSchema();// store.getState().adminForms.formEditor.saveSchema();
    } else {
      return currentFormBuilder._form; //store.getState().adminForms.formBuilder._form;
    }
  }
  saveCurrentForm = () => {
    let form = JSON.parse(JSON.stringify(store.getState().adminForms.currentForm));
    if (currentFormEditor) {
      form.schema = currentFormEditor.saveSchema();
    } else {
      form.schema = currentFormBuilder._form;
    }
    form.previewData = JSON.parse(form.previewData);
    api.post('/edition/forms', form).then(response => {
      form.modified = response.data.modified;
    }).catch(error => {
      alert(error.message);
    })
  }
  setFormPreview = (data: string): AppThunk => async dispatch => {
    dispatch(setCurrentFormPreview(data));
  }
  getCurrentForm = (): any => {
    return store.getState().adminForms.currentForm;
  }

  buildEditor = (div: any, form:any): AppThunk => async dispatch => {
    if (!this.formEditorCreating) {
      this.formEditorCreating = true;

    if (this.lastFormEditor) {
      this.lastFormEditor.destroy();
    }

      let div = document.querySelector('#form-editor');

      if (form.generator == 'formJs') {
        this.lastFormEditor = newFormEditor({
          container: div
        });
        this.lastFormEditor.importSchema(form.schema);
        dispatch(adminFormService.setFormEditor(this.lastFormEditor));
      } else {
        const formBuilder = new FormBuilder(div, JSON.parse(JSON.stringify(form.schema)), { noDefaultSubmitButton: true });
        dispatch(adminFormService.setFormBuilder(formBuilder));
      }

      this.formEditorCreating = false;
    }
  }
}

const adminFormService = new AdminFormService();

export default adminFormService;
