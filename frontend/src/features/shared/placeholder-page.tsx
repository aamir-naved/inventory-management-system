type PlaceholderPageProps = {
  title: string;
  description: string;
};

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <section className="empty-state">
      <span className="brand-kicker">Scaffolded route</span>
      <h1>{title}</h1>
      <p>{description}</p>
    </section>
  );
}
