import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {
  private readonly _prefix: string;
  private readonly _allowNull: boolean;

  constructor() {
    this._prefix = 'mydocker';
    this._allowNull = false;
  }

  /**
   * Gets the number of entries in the applications local storage.
   * @returns
   */
  count(): number | undefined {
    try {
      return localStorage.length;
    } catch (error) {
      console.error(error);
      return 0;
    }
  }

  /**
   * Returns the nth (defined by the index parameter) key in the storage.
   * The order of keys is user-agent defined, so you should not rely on it.
   * @param index   An integer representing the number of the key you want to get the name of. This is a zero-based index.
   * @returns
   */
  getKey(index: number): string | null | undefined {
    if (index < 0) {
      console.error(new Error('index has to be 0 or greater'));
      return null;
    }
    try {
      return localStorage.key(index);
    } catch (error) {
      console.error(error);
      return null;
    }
  }

  /**
   * Adds tha value with the given key or updates an existing entry.
   * @param key     Key to store.
   * @param value   Value to store.
   * @param prefix  Optional prefix to overwrite the configured one.
   * @returns
   */
  set(key: string, value: string, prefix?: string): void {
    if (this._allowNull
      || (!this._allowNull && value !== 'null' && value !== null && value !== undefined)) {
      localStorage.setItem(`${prefix || this._prefix}_${key}`, value);
    } else {
      this.remove(key, prefix);
    }
  }

  /**
   * Gets the entry specified by the given key or null.
   * @param key     Key identifying the wanted entry.
   * @param prefix  Optional prefix to overwrite the configured one.
   * @returns
   */
  get(key: string, prefix?: string): string | null | undefined {
    try {
      return localStorage.getItem(`${prefix || this._prefix}_${key}`);
    } catch (error) {
      console.error(error);
      return null;
    }
  }

  /**
   * Removes the entry specified by the given key.
   * @param key     Key identifying the entry to remove.
   * @param prefix  Optional prefix to overwrite the configured one.
   * @returns
   */
  remove(key: string, prefix?: string): void {
    try {
      localStorage.removeItem(`${prefix || this._prefix}_${key}`);
    } catch (error) {
      console.error(error);
    }
  }

  /**
   * Clears all entries of the applications local storage.
   * @returns
   */
  clear(): void {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch (error) {
      console.error(error);
    }
  }

  sessionSet(key:string, value: string, prefix?: string): void {
    sessionStorage.setItem(`${prefix || this._prefix}_${key}`, value);
  }

  sessionGetAndRemove(key:string, prefix?: string): string | null {
    const keyWithPrefix = `${prefix || this._prefix}_${key}`;
    const value = sessionStorage.getItem(keyWithPrefix);
    sessionStorage.removeItem(keyWithPrefix);
    return value;
  }
}
