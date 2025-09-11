export interface IJWTToken {
  encodedToken: string;
  decodedToken: {
    exp: number;
    iat: number;
    iss: string;
    jti: string;
    nbf: number;
    sub: string;
    email: null | string;
    origin: TokenOrigin;
  };
  expirationDate: Date;
}

export enum TokenOrigin {
  CAS = 'CAS',
  LTI = 'LTI',
  OIDC = 'OIDC',
}
