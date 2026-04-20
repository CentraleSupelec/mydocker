import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent implements OnInit {
  title = 'NewMyDockerFront';
  constructor(private translate: TranslateService) {
  }

  async ngOnInit() {
    const storedLang = localStorage.getItem('language');
    if (storedLang) {
      this.translate.use(storedLang);
    } else {
      const browserLang = this.translate.getBrowserLang();
      const defaultLang = browserLang?.match(/fr|en/) ? browserLang : 'fr';
      this.translate.use(defaultLang);
      localStorage.setItem('language', defaultLang);
    }
  }
}
