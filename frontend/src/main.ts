import { registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

registerLocaleData(localePl);

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
